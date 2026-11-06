package com.geopslabs.geops.engagement.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record ReplyRequest(@NotBlank @Size(max = 2000) String text) {}
