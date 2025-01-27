package org.apereo.cas.services;

import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.HttpUtils;
import org.apereo.cas.util.LoggingUtils;
import org.apereo.cas.util.serialization.JacksonObjectMapperFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.http.HttpResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.StringWriter;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;

/**
 * A proxy policy that only allows proxying to pgt urls
 * via a REST endpoint.
 *
 * @author Misagh Moayyed
 * @since 6.6.0
 */
@Slf4j
@ToString
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class RestfulRegisteredServiceProxyPolicy implements RegisteredServiceProxyPolicy {

    private static final long serialVersionUID = -222069319543047324L;

    private static final ObjectMapper MAPPER = JacksonObjectMapperFactory.builder()
        .singleValueAsArray(true).defaultTypingEnabled(true).build().toObjectMapper();

    private String endpoint;

    private Map<String, Object> headers = new TreeMap<>();

    @JsonIgnore
    @Override
    public boolean isAllowedToProxy() {
        return true;
    }

    @Override
    public boolean isAllowedProxyCallbackUrl(final RegisteredService registeredService, final URL pgtUrl) {
        HttpResponse response = null;
        try (val writer = new StringWriter()) {
            MAPPER.writer(new MinimalPrettyPrinter()).writeValue(writer, registeredService);
            val exec = HttpUtils.HttpExecutionRequest.builder()
                .method(HttpMethod.GET)
                .headers(headers)
                .url(this.endpoint)
                .entity(writer.toString())
                .parameters(CollectionUtils.wrap("pgtUrl", pgtUrl.toExternalForm()))
                .headers(CollectionUtils.wrap("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .build();
            response = HttpUtils.execute(exec);
            return HttpStatus.valueOf(response.getStatusLine().getStatusCode()).is2xxSuccessful();
        } catch (final Exception e) {
            LoggingUtils.error(LOGGER, e);
        } finally {
            HttpUtils.close(response);
        }
        return false;
    }
}
