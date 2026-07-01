package uk.ac.cam.cl.dtg.segue.etl;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapperUtils;

class ContentElasticSearchSubmitterTest {

  @Test
  void constructor_createsInstance() {
    ContentMapperUtils mapperUtils = new ContentMapperUtils();

    ContentElasticSearchSubmitter submitter = new ContentElasticSearchSubmitter(null, mapperUtils);

    assertNotNull(submitter);
  }
}