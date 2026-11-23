"""Writes the result of one service's build to the job summary: tests per Surefire class and Cucumber scenarios.

Usage: python test-summary.py <service-dir>. Appends Markdown to $GITHUB_STEP_SUMMARY (or prints it). It never fails
the job: whether the build passes is decided by ./mvnw verify.
"""
import glob
import json
import os
import sys
import xml.etree.ElementTree as ElementTree


def surefire_totals(service):
    totals = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
    failed = []
    for path in sorted(glob.glob(os.path.join(service, "target", "surefire-reports", "TEST-*.xml"))):
        suite = ElementTree.parse(path).getroot()
        for key in totals:
            totals[key] += int(suite.get(key, 0))
        for case in suite.iter("testcase"):
            if case.find("failure") is not None or case.find("error") is not None:
                failed.append(f"{case.get('classname')}.{case.get('name')}")
    return totals, failed


def cucumber_scenarios(service):
    path = os.path.join(service, "target", "cucumber-reports", "cucumber.json")
    if not os.path.exists(path):
        return None
    with open(path, encoding="utf-8") as report:
        features = json.load(report)
    scenarios = []
    for feature in features:
        for element in feature.get("elements", []):
            if element.get("type") != "scenario":
                continue
            statuses = [step.get("result", {}).get("status") for step in element.get("steps", [])]
            statuses += [hook.get("result", {}).get("status") for hook in element.get("before", []) + element.get("after", [])]
            if any(status == "failed" for status in statuses):
                status = "failed"
            elif statuses and all(status == "passed" for status in statuses):
                status = "passed"
            else:
                status = "skipped"
            scenarios.append((feature.get("name", ""), element.get("name", ""), status))
    return scenarios


def cell(text):
    return " ".join(text.split()).replace("|", "\\|")


def main():
    service = sys.argv[1].rstrip("/")
    totals, failed = surefire_totals(service)
    lines = [f"### {os.path.basename(service)}", "",
             f"Pruebas: {totals['tests']} | fallidas: {totals['failures']} | con error: {totals['errors']} "
             f"| omitidas: {totals['skipped']}", ""]
    for name in failed:
        lines.append(f"- falla `{name}`")
    if failed:
        lines.append("")
    scenarios = cucumber_scenarios(service)
    if scenarios is None:
        lines.append("Sin reporte de Cucumber.")
    else:
        counts = {status: sum(1 for _, _, s in scenarios if s == status) for status in ("passed", "failed", "skipped")}
        lines.append(f"Escenarios BDD: {len(scenarios)} | pasan: {counts['passed']} | fallan: {counts['failed']} "
                     f"| sin ejecutar: {counts['skipped']}")
        if scenarios:
            lines += ["", "| Feature | Escenario | Resultado |", "|---|---|---|"]
        for feature, scenario, status in scenarios:
            lines.append(f"| {cell(feature)} | {cell(scenario)} | {status} |")
    lines.append("")
    text = "\n".join(lines) + "\n"
    target = os.environ.get("GITHUB_STEP_SUMMARY")
    if target:
        with open(target, "a", encoding="utf-8") as summary:
            summary.write(text)
    else:
        sys.stdout.write(text)


if __name__ == "__main__":
    main()
