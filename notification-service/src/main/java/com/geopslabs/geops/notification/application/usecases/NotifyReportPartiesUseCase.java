package com.geopslabs.geops.notification.application.usecases;

import com.geopslabs.geops.notification.domain.models.ReportNoticeEvent;
public interface NotifyReportPartiesUseCase { void apply(ReportNoticeEvent event); }
