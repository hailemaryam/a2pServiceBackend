package org.hmmk.sms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsSentBySource {

    @JsonProperty("API")
    public long api;

    @JsonProperty("MANUAL")
    public long manual;

    @JsonProperty("CSV_UPLOAD")
    public long csvUpload;
}

