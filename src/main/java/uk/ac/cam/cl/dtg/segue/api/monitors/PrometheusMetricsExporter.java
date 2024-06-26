/**
 * Copyright 2018 Meurig Thomas
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.segue.api.monitors;

import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by mlt47 on 09/03/2018.
 */
public class PrometheusMetricsExporter implements IMetricsExporter {
  private static final Logger log = LoggerFactory.getLogger(PrometheusMetricsExporter.class);

  /**
   * Constructs a metrics exporter on the port specified for reporting custom logic in a Prometheus style.
   *
   * @param port the port to expose the metrics.
   * @throws IOException could be thrown by the socket.
   */
  @SuppressWarnings("java:S2095")
  public PrometheusMetricsExporter(final int port) throws IOException {
    // IMPORTANT: it has been suggested that the grafana issues seen during a previous dependency update attempt were
    // the result of applying a try-catch pattern to this resource, causing the metrics server to close prematurely.
    // Do not apply this linter recommendation at this time.
    new HTTPServer(port);
    log.info("Prometheus Metrics Exporter has been initialised on port {}", port);
  }

  @Override
  public void exposeJvmMetrics() {
    DefaultExports.initialize();
  }
}
