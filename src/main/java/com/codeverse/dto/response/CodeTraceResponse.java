
package com.codeverse.dto.response;

import java.util.List;
import java.util.Map;

public record CodeTraceResponse(

        String language,
        boolean success,
        List<TraceStep> steps,
        String output,
        String error

) {

    public record TraceStep(

            int stepNumber,
            int lineNumber,
            String event,
            Map<String, Object> variables,
            String output,
            String explanation

    ) {}

}
