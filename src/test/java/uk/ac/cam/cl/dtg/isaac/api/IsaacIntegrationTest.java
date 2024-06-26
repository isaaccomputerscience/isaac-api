package uk.ac.cam.cl.dtg.isaac.api;

import static java.time.ZoneOffset.UTC;
import static org.easymock.EasyMock.and;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_ADMIN_ID;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_DATE_FORMAT;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_LINUX_CONFIG_LOCATION;
import static uk.ac.cam.cl.dtg.segue.api.Constants.EMAIL_SIGNATURE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.HMAC_SALT;
import static uk.ac.cam.cl.dtg.segue.api.Constants.HOST_NAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.NUMBER_SECONDS_IN_FIVE_MINUTES;
import static uk.ac.cam.cl.dtg.segue.dao.content.ContentMapperUtils.getSharedBasicObjectMapper;
import static uk.ac.cam.cl.dtg.util.ReflectionUtils.getClasses;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.api.client.util.Maps;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.SystemUtils;
import org.easymock.Capture;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import uk.ac.cam.cl.dtg.isaac.api.managers.AssignmentManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventBookingManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.QuizAssignmentManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.QuizAttemptManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.QuizManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.QuizQuestionManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.URIManager;
import uk.ac.cam.cl.dtg.isaac.api.services.AssignmentService;
import uk.ac.cam.cl.dtg.isaac.api.services.ContentSummarizerService;
import uk.ac.cam.cl.dtg.isaac.api.services.EmailService;
import uk.ac.cam.cl.dtg.isaac.dao.EventBookingPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dao.GameboardPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dao.IAssignmentPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dao.IQuizAssignmentPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dao.IQuizAttemptPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dao.IQuizQuestionAttemptPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dao.PgAssignmentPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dao.PgQuizAssignmentPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dao.PgQuizAttemptPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dao.PgQuizQuestionAttemptPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.AbstractUserPreferenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.PgUserPreferenceManager;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.mappers.MainObjectMapper;
import uk.ac.cam.cl.dtg.isaac.quiz.PgQuestionAttempts;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.PgTransactionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.RecaptchaManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAuthenticationManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserBadgeManager;
import uk.ac.cam.cl.dtg.segue.api.monitors.GroupManagerLookupMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.IMisuseMonitor;
import uk.ac.cam.cl.dtg.segue.api.monitors.InMemoryMisuseMonitor;
import uk.ac.cam.cl.dtg.segue.api.services.ContentService;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.auth.IAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.ISecondFactorAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.ISegueHashingAlgorithm;
import uk.ac.cam.cl.dtg.segue.auth.SegueLocalAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.SeguePBKDF2v3;
import uk.ac.cam.cl.dtg.segue.auth.SegueSCryptv1;
import uk.ac.cam.cl.dtg.segue.auth.SegueTOTPAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AdditionalAuthenticationRequiredException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticationProviderMappingException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.IncorrectCredentialsProvidedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.MFARequiredButNotConfiguredException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoCredentialsAvailableException;
import uk.ac.cam.cl.dtg.segue.comm.EmailCommunicator;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.associations.PgAssociationDataManager;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapperUtils;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.dao.schools.SchoolListReader;
import uk.ac.cam.cl.dtg.segue.dao.users.PgAnonymousUsers;
import uk.ac.cam.cl.dtg.segue.dao.users.PgPasswordDataManager;
import uk.ac.cam.cl.dtg.segue.dao.users.PgUserGroupPersistenceManager;
import uk.ac.cam.cl.dtg.segue.dao.users.PgUsers;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.search.ElasticSearchProvider;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

/**
 * Abstract superclass for integration tests, providing them with dependencies including ElasticSearch and PostgreSQL
 * (as docker containers) and other managers (some of which are mocked). These dependencies are created before and
 * destroyed after every test class.
 * <br>
 * Subclasses should be named "*IT.java" so Maven Failsafe detects them. They are runnable via the "verify" Maven target.
 */
public abstract class IsaacIntegrationTest {

  protected static HttpSession httpSession;
  protected static PostgreSQLContainer<?> postgres;
  protected static ElasticsearchContainer elasticsearch;
  protected static PropertiesLoader properties;
  protected static Map<String, String> globalTokens;
  protected static PostgresSqlDb postgresSqlDb;
  protected static ElasticSearchProvider elasticSearchProvider;
  protected static SchoolListReader schoolListReader;
  protected static MainObjectMapper mainObjectMapper;
  protected static Map<AuthenticationProvider, IAuthenticator> providersToRegister;
  protected static IMisuseMonitor misuseMonitor;

  // Managers
  protected static EmailManager emailManager;
  protected static AbstractUserPreferenceManager userPreferenceManager;
  protected static UserAuthenticationManager userAuthenticationManager;
  protected static ISecondFactorAuthenticator secondFactorManager;
  protected static UserAccountManager userAccountManager;
  protected static RecaptchaManager recaptchaManager;
  protected static GameManager gameManager;
  protected static GroupManager groupManager;
  protected static EventBookingManager eventBookingManager;
  protected static ILogManager logManager;
  protected static GitContentManager contentManager;
  protected static UserBadgeManager userBadgeManager;
  protected static UserAssociationManager userAssociationManager;
  protected static AssignmentManager assignmentManager;
  protected static QuestionManager questionManager;
  protected static QuizManager quizManager;

  // Manager dependencies
  protected static IQuizAssignmentPersistenceManager quizAssignmentPersistenceManager;
  protected static QuizAssignmentManager quizAssignmentManager;
  protected static QuizAttemptManager quizAttemptManager;
  protected static IQuizAttemptPersistenceManager quizAttemptPersistenceManager;
  protected static IQuizQuestionAttemptPersistenceManager quizQuestionAttemptPersistenceManager;
  protected static QuizQuestionManager quizQuestionManager;
  protected static PgUsers pgUsers;
  protected static PgAnonymousUsers pgAnonymousUsers;
  protected static ContentMapperUtils contentMapperUtils;

  // Services
  protected static AssignmentService assignmentService;

  @BeforeAll
  public static void setUpClass() {
    postgres = new PostgreSQLContainer<>("postgres:14-alpine")
        .withEnv("POSTGRES_HOST_AUTH_METHOD", "trust")
        .withUsername("rutherford")
        .withFileSystemBind(
            IsaacIntegrationTest.class.getClassLoader().getResource("db_scripts/postgres-rutherford-create-script.sql")
                .getPath(), "/docker-entrypoint-initdb.d/00-isaac-create.sql")
        .withFileSystemBind(
            IsaacIntegrationTest.class.getClassLoader().getResource("db_scripts/postgres-rutherford-functions.sql")
                .getPath(), "/docker-entrypoint-initdb.d/01-isaac-functions.sql")
        .withFileSystemBind(
            IsaacIntegrationTest.class.getClassLoader().getResource("db_scripts/quartz_scheduler_create_script.sql")
                .getPath(), "/docker-entrypoint-initdb.d/02-isaac-quartz.sql")
        .withFileSystemBind(
            IsaacIntegrationTest.class.getClassLoader().getResource("test-postgres-rutherford-data-dump.sql").getPath(),
            "/docker-entrypoint-initdb.d/03-data-dump.sql")
    ;

    // TODO It would be nice if we could pull the version from pom.xml
    elasticsearch =
        new ElasticsearchContainer(DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:7.17.6"))
            .withCopyFileToContainer(MountableFile.forClasspathResource("isaac-test-es-data.tar.gz"),
                "/usr/share/elasticsearch/isaac-test-es-data.tar.gz")
            .withCopyFileToContainer(MountableFile.forClasspathResource("isaac-test-es-docker-entrypoint.sh", 0100775),
                "/usr/local/bin/docker-entrypoint.sh")
            .withExposedPorts(9200, 9300)
            .withEnv("cluster.name", "isaac")
            .withEnv("node.name", "localhost")
            .withEnv("http.max_content_length", "512mb")
            .withEnv("xpack.security.enabled", "true")
            .withEnv("ELASTIC_PASSWORD", "elastic")
            .withEnv("ingest.geoip.downloader.enabled", "false")
            .withStartupTimeout(Duration.ofSeconds(120));

    postgres.start();
    elasticsearch.start();

    postgresSqlDb = new PostgresSqlDb(
        postgres.getJdbcUrl(),
        "rutherford",
        "somerandompassword"
    ); // user/pass are irrelevant because POSTGRES_HOST_AUTH_METHOD is set to "trust"

    try {
      elasticSearchProvider =
          new ElasticSearchProvider(ElasticSearchProvider.getClient(
              "localhost",
              elasticsearch.getMappedPort(9200),
              "elastic",
              "elastic"
          )
          );
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }


    String configLocation = SystemUtils.IS_OS_LINUX ? DEFAULT_LINUX_CONFIG_LOCATION : null;
    if (System.getProperty("test.config.location") != null) {
      configLocation = System.getProperty("test.config.location");
    }
    if (System.getenv("SEGUE_TEST_CONFIG_LOCATION") != null) {
      configLocation = System.getenv("SEGUE_TEST_CONFIG_LOCATION");
    }

    try {
      properties = new PropertiesLoader(configLocation);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    globalTokens = Maps.newHashMap();
    globalTokens.put("sig", properties.getProperty(EMAIL_SIGNATURE));
    globalTokens.put("emailPreferencesURL",
        String.format("https://%s/account#emailpreferences", properties.getProperty(HOST_NAME)));
    globalTokens.put("myAssignmentsURL", String.format("https://%s/assignments", properties.getProperty(HOST_NAME)));
    globalTokens.put("myQuizzesURL", String.format("https://%s/quizzes", properties.getProperty(HOST_NAME)));
    globalTokens.put("myBookedEventsURL",
        String.format("https://%s/events?show_booked_only=true", properties.getProperty(HOST_NAME)));
    globalTokens.put("contactUsURL", String.format("https://%s/contact", properties.getProperty(HOST_NAME)));
    globalTokens.put("accountURL", String.format("https://%s/account", properties.getProperty(HOST_NAME)));
    globalTokens.put("siteBaseURL", String.format("https://%s", properties.getProperty(HOST_NAME)));

    recaptchaManager = new RecaptchaManager(properties);

    JsonMapper jsonMapper = new JsonMapper();
    pgUsers = new PgUsers(postgresSqlDb, jsonMapper);
    pgAnonymousUsers = new PgAnonymousUsers(postgresSqlDb);
    PgPasswordDataManager passwordDataManager = new PgPasswordDataManager(postgresSqlDb);

    contentMapperUtils = new ContentMapperUtils(getClasses("uk.ac.cam.cl.dtg"));
    PgQuestionAttempts pgQuestionAttempts = new PgQuestionAttempts(postgresSqlDb, contentMapperUtils);

    mainObjectMapper = MainObjectMapper.INSTANCE;

    questionManager = new QuestionManager(contentMapperUtils, mainObjectMapper, pgQuestionAttempts, userPreferenceManager);

    // The following may need some actual authentication providers...
    providersToRegister = new HashMap<>();
    Map<String, ISegueHashingAlgorithm> algorithms =
        new HashMap<>(Map.of("SeguePBKDF2v3", new SeguePBKDF2v3(), "SegueSCryptv1", new SegueSCryptv1()));
    providersToRegister.put(AuthenticationProvider.SEGUE,
        new SegueLocalAuthenticator(pgUsers, passwordDataManager, properties, algorithms,
            algorithms.get("SegueSCryptv1")));

    EmailCommunicator communicator =
        new EmailCommunicator("localhost", "587", null, null, "default@localhost", "Howdy!");
    userPreferenceManager = new PgUserPreferenceManager(postgresSqlDb);

    Git git = createNiceMock(Git.class);
    GitDb gitDb = new GitDb(git);
    contentManager = new GitContentManager(gitDb, elasticSearchProvider, contentMapperUtils, mainObjectMapper, properties);
    logManager = createNiceMock(ILogManager.class);

    emailManager =
        new EmailManager(communicator, userPreferenceManager, properties, contentManager, logManager, globalTokens);

    userAuthenticationManager = new UserAuthenticationManager(pgUsers, properties, providersToRegister, emailManager);
    secondFactorManager = createMock(SegueTOTPAuthenticator.class);
    // We don't care for MFA here so we can safely disable it
    try {
      expect(secondFactorManager.has2FAConfigured(anyObject())).andReturn(false).atLeastOnce();
    } catch (SegueDatabaseException e) {
      throw new RuntimeException(e);
    }
    replay(secondFactorManager);

    schoolListReader = new SchoolListReader(elasticSearchProvider);

    userAccountManager =
        new UserAccountManager(pgUsers, questionManager, properties, providersToRegister, mainObjectMapper, emailManager,
            pgAnonymousUsers, logManager, userAuthenticationManager, secondFactorManager, userPreferenceManager,
            schoolListReader);

    ObjectMapper objectMapper = getSharedBasicObjectMapper();
    EventBookingPersistenceManager bookingPersistanceManager =
        new EventBookingPersistenceManager(postgresSqlDb, userAccountManager, contentManager, objectMapper);
    PgAssociationDataManager pgAssociationDataManager = new PgAssociationDataManager(postgresSqlDb);
    PgUserGroupPersistenceManager pgUserGroupPersistenceManager = new PgUserGroupPersistenceManager(postgresSqlDb);
    IAssignmentPersistenceManager assignmentPersistenceManager =
        new PgAssignmentPersistenceManager(postgresSqlDb, mainObjectMapper);

    GameboardPersistenceManager gameboardPersistenceManager =
        new GameboardPersistenceManager(postgresSqlDb, contentManager, mainObjectMapper, objectMapper,
            new URIManager(properties));
    gameManager = new GameManager(contentManager, gameboardPersistenceManager, mainObjectMapper, questionManager);
    groupManager = new GroupManager(pgUserGroupPersistenceManager, userAccountManager, gameManager, mainObjectMapper);
    userAssociationManager = new UserAssociationManager(pgAssociationDataManager, userAccountManager, groupManager);
    PgTransactionManager pgTransactionManager = new PgTransactionManager(postgresSqlDb);
    eventBookingManager =
        new EventBookingManager(bookingPersistanceManager, emailManager, userAssociationManager, properties,
            groupManager, userAccountManager, pgTransactionManager);
    userBadgeManager = createNiceMock(UserBadgeManager.class);
    replay(userBadgeManager);
    assignmentManager = new AssignmentManager(assignmentPersistenceManager, groupManager,
        new EmailService(emailManager, groupManager, userAccountManager), gameManager, properties);

    quizManager = new QuizManager(properties, new ContentService(contentManager), contentManager,
        new ContentSummarizerService(mainObjectMapper, new URIManager(properties)), contentMapperUtils);
    quizAssignmentPersistenceManager = new PgQuizAssignmentPersistenceManager(postgresSqlDb, mainObjectMapper);
    quizAssignmentManager = new QuizAssignmentManager(quizAssignmentPersistenceManager,
        new EmailService(emailManager, groupManager, userAccountManager), quizManager, groupManager, properties);
    assignmentService = new AssignmentService(userAccountManager);
    quizAttemptPersistenceManager = new PgQuizAttemptPersistenceManager(postgresSqlDb, mainObjectMapper);
    quizAttemptManager = new QuizAttemptManager(quizAttemptPersistenceManager);
    quizQuestionAttemptPersistenceManager = new PgQuizQuestionAttemptPersistenceManager(postgresSqlDb,
        contentMapperUtils);
    quizQuestionManager =
        new QuizQuestionManager(questionManager, mainObjectMapper, quizQuestionAttemptPersistenceManager, quizManager,
            quizAttemptManager);

    misuseMonitor = new InMemoryMisuseMonitor();
    misuseMonitor.registerHandler(GroupManagerLookupMisuseHandler.class.getSimpleName(),
        new GroupManagerLookupMisuseHandler(emailManager, properties));

    String someSegueAnonymousUserId = "9284723987anonymous83924923";
    httpSession = createNiceMock(HttpSession.class);
    expect(httpSession.getAttribute(Constants.ANONYMOUS_USER)).andReturn(null).anyTimes();
    expect(httpSession.getId()).andReturn(someSegueAnonymousUserId).anyTimes();
    replay(httpSession);

    // NOTE: The next part is commented out until we figure out a way of actually using Guice to do the heavy lifting for us...
    /*
    // Create Mocked Injector
    SegueGuiceConfigurationModule.setGlobalPropertiesIfNotSet(properties);
    Module productionModule = new SegueGuiceConfigurationModule();
    Module testModule = Modules.override(productionModule).with(new AbstractModule() {
        @Override protected void configure() {
            // ... register mocks
            bind(UserAccountManager.class).toInstance(userAccountManager);
            bind(GameManager.class).toInstance(createNiceMock(GameManager.class));
            bind(GroupChangedService.class).toInstance(createNiceMock(GroupChangedService.class));
            bind(EventBookingManager.class).toInstance(eventBookingManager);
        }
    });
    Injector injector = Guice.createInjector(testModule);
     */
  }

  @AfterAll
  static void tearDownClass() {
    postgres.stop();
    elasticsearch.stop();
  }

  protected LoginResult loginAs(final HttpSession httpSession, final String username, final String password)
      throws NoCredentialsAvailableException, SegueDatabaseException, AuthenticationProviderMappingException,
      IncorrectCredentialsProvidedException, AdditionalAuthenticationRequiredException, InvalidKeySpecException,
      NoSuchAlgorithmException, MFARequiredButNotConfiguredException {
    Capture<Cookie> capturedUserCookie = Capture.newInstance(); // new Capture<Cookie>(); seems deprecated

    HttpServletRequest userLoginRequest = createNiceMock(HttpServletRequest.class);
    expect(userLoginRequest.getSession()).andReturn(httpSession).atLeastOnce();
    replay(userLoginRequest);

    HttpServletResponse userLoginResponse = createNiceMock(HttpServletResponse.class);
    userLoginResponse.addCookie(and(capture(capturedUserCookie), isA(Cookie.class)));
    expectLastCall().atLeastOnce(); // This is how you expect void methods, apparently...
    replay(userLoginResponse);

    RegisteredUserDTO user = userAccountManager.authenticateWithCredentials(userLoginRequest, userLoginResponse,
        AuthenticationProvider.SEGUE.toString(), username, password);

    return new LoginResult(user, capturedUserCookie.getValue());
  }

  protected HttpServletRequest createRequestWithCookies(final Cookie[] cookies) {
    HttpServletRequest request = createNiceMock(HttpServletRequest.class);
    expect(request.getCookies()).andReturn(cookies).anyTimes();
    return request;
  }

  protected static class LoginResult {
    public RegisteredUserDTO user;
    public Cookie cookie;

    public LoginResult(final RegisteredUserDTO user, final Cookie cookie) {
      this.user = user;
      this.cookie = cookie;
    }
  }

  /**
   * As the integration tests do not currently support MFA login, we cannot use the normal login process and have to
   * create cookies manually when testing admin accounts.
   *
   * @return a Cookie loaded with session information for the test admin user.
   * @throws JsonProcessingException if the cookie serialisation fails
   */
  protected Cookie createManualCookieForAdmin() throws JsonProcessingException {
    DateTimeFormatter sessionDateFormat = DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT).withZone(UTC);
    String userId = String.valueOf(TEST_ADMIN_ID);
    String hmacKey = properties.getProperty(HMAC_SALT);
    int sessionExpiryTimeInSeconds = NUMBER_SECONDS_IN_FIVE_MINUTES;

    String sessionExpiryDate = sessionDateFormat.format(Instant.now().plusSeconds(sessionExpiryTimeInSeconds));

    Map<String, String> sessionInformation =
        userAuthenticationManager.prepareSessionInformation(userId, "0", sessionExpiryDate, hmacKey, null);
    return userAuthenticationManager.createAuthCookie(sessionInformation, sessionExpiryTimeInSeconds);
  }

  protected HttpServletRequest prepareAdminRequest() {
    try {
      Cookie adminSessionCookie = createManualCookieForAdmin();
      HttpServletRequest adminRequest = createRequestWithCookies(new Cookie[] {adminSessionCookie});
      replay(adminRequest);
      return adminRequest;
    } catch (JsonProcessingException e) {
      fail(e);
      return null;
    }
  }
}
