/**
 * Copyright 2025 Isaac Computer Science
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

package uk.ac.cam.cl.dtg.segue.dao.schools;

import static uk.ac.cam.cl.dtg.util.LogUtils.sanitiseExternalLogValue;

import com.google.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.users.School;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

/**
 * Temporary fallback lookup for schools that are no longer in Elasticsearch.
 * This queries the schools_2022 table in the database to resolve school names
 * for users who registered with schools that have since been removed from the
 * current school list.
 */
public class PgSchoolLookup {
  private static final Logger log = LoggerFactory.getLogger(PgSchoolLookup.class);

  private final PostgresSqlDb database;

  /**
   * Constructor for PgSchoolLookup.
   *
   * @param database - the postgres datasource to use
   */
  @Inject
  public PgSchoolLookup(final PostgresSqlDb database) {
    this.database = database;
  }

  /**
   * Look up a school by URN from the schools_2022 database table.
   * This is a fallback for schools that no longer exist in the current Elasticsearch index.
   *
   * @param schoolUrn - the URN of the school to look up
   * @return a School object if found, or null if not found
   * @throws SegueDatabaseException if there is a database error
   */
  public School findSchoolById(final String schoolUrn) throws SegueDatabaseException {
    if (schoolUrn == null || schoolUrn.isEmpty()) {
      return null;
    }

    String query = "SELECT urn, school_name FROM schools_2022 WHERE urn = ?";

    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)) {
      pst.setString(1, schoolUrn);

      try (ResultSet results = pst.executeQuery()) {
        if (results.next()) {
          School school = new School();
          school.setUrn(results.getString("urn"));
          school.setName(results.getString("school_name"));
          school.setClosed(true);
          school.setDataSource(School.SchoolDataSource.GOVERNMENT_UK);
          log.debug("Found school {} in fallback database table schools_2022",
              sanitiseExternalLogValue(schoolUrn));
          return school;
        }
      }
    } catch (SQLException e) {
      String errorMsg = String.format("Error looking up school with URN %s from schools_2022 table",
          sanitiseExternalLogValue(schoolUrn));
      log.error(errorMsg, e);
      throw new SegueDatabaseException(errorMsg, e);
    }

    log.debug("School with URN {} not found in fallback database table schools_2022",
        sanitiseExternalLogValue(schoolUrn));
    return null;
  }
}
