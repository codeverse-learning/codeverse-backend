
package com.codeverse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeExplainResponse {

    private String language;

    private String explanationLevel;

    private String explanation;

    private String model;
}
