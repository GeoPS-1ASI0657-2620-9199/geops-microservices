-- Avisos de reporte guardados antes de corregir NotificationFactoryService: el texto perdio las tildes
-- ("report?", "respondi?", "cerr?") y asi se ven en la campana. Los textos nuevos ya salen bien.
-- Las tildes van como escapes Unicode (U&'\00f3' = o con tilde) para que el script sea ASCII y no dependa
-- de la codificacion con que se lea.
UPDATE notifications
SET title = replace(title, 'Se cerr? tu reporte', U&'Se cerr\00f3 tu reporte'),
    message = replace(replace(replace(message,
        ' report? ', U&' report\00f3 '),
        ' respondi? ', U&' respondi\00f3 '),
        ' cerr? ', U&' cerr\00f3 ')
WHERE notification_type IN ('REPORT_RECEIVED', 'REPORT_ANSWERED', 'REPORT_RESOLVED')
  AND (title LIKE '%?%' OR message LIKE '%?%');
