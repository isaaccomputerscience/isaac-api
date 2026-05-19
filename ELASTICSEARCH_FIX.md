# Elasticsearch Integration Test Fix

## Problem
9 integration tests (AssignmentFacadeIT, AuthenticationFacadeIT, etc.) were failing with:
```
Could not start container
java.lang.NullPointerException: Cannot invoke "jdk.internal.platform.CgroupInfo.getMountPoint()" because "anyController" is null
```

**Root Cause**: Java 17 cgroup detection fails in containerized environments (GitHub Actions Ubuntu runners), causing Elasticsearch 7.17.6 to crash during startup.

## Current Implementation

### ✅ Option 2: Quick Fix (ACTIVE)
**Elasticsearch 7.17.6 + JVM Options**

Added to `src/test/java/uk/ac/cam/cl/dtg/isaac/api/IsaacIntegrationTest.java`:
```java
.withEnv("ES_JAVA_OPTS", "-Dcom.sun.management.jmxremote=false -XX:+IgnoreUnrecognizedVMOptions")
```

**Why this works**:
- Disables JMX reconfiguration that triggers cgroup detection
- Ignores unrecognized VM options for compatibility
- No test data regeneration needed
- Minimal changes, maximum reliability

**How to test**: 
```bash
mvn -B verify
```

---

## Option 1: Long-term Upgrade (Available)

To upgrade to **Elasticsearch 8.13.0** (better Java 17 support, long-term maintenance):

### Step 1: Update Version in Integration Tests
In `src/test/java/uk/ac/cam/cl/dtg/isaac/api/IsaacIntegrationTest.java`, line 198:
```java
// Change from:
DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:7.17.6")

// To:
DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.13.0")
```

### Step 2: Update Local Development Compose File
In `compose-local-deps.yml`, update both elasticsearch and cs-elasticsearch services:
```yaml
image: docker.elastic.co/elasticsearch/elasticsearch:8.13.0
```

### Step 3: Test Data Migration
The test data (isaac-test-es-data.tar.gz) was created for ES 7.x.

**Try first**: ES 8.13 may auto-migrate indices:
```bash
mvn -B verify
```

**If tests fail with index errors**:
1. Regenerate test data (requires setting up test data population):
   ```bash
   # Comment out tar extraction in isaac-test-es-docker-entrypoint.sh
   # Let ES 8.13 start with empty data
   # Populate it with your ETL/test setup scripts
   src/test/resources/elasticsearch_export.sh  # Export new data
   ```

---

## Summary

| Aspect | Option 2 (Current) | Option 1 (Available) |
|--------|-------------------|----------------------|
| ES Version | 7.17.6 | 8.13.0 |
| Changes | 1 environment variable | Version updates in 2 files |
| Test Data | No regeneration | May need regeneration |
| Long-term Support | ⚠️ Limited | ✅ Good |
| Testing Effort | Minimal | Moderate |
| Risk | Very Low | Low (auto-migration likely works) |

## Switching Between Options

### To use Option 2 (7.17.6 + JVM options) - Current
```bash
# Already implemented, just run:
mvn -B verify
```

### To use Option 1 (8.13.0) - Upgrade
```bash
# Edit src/test/java/uk/ac/cam/cl/dtg/isaac/api/IsaacIntegrationTest.java
# Change elasticsearch version to 8.13.0

# Edit compose-local-deps.yml
# Change both elasticsearch services to 8.13.0

# Then test:
mvn -B verify
```

If ES 8.13 tests fail, revert to 7.17.6 and stick with Option 2.

---

## Technical Details

**Why Java 17 + ES 7.17.6 fails**:
- Java 17 tries to detect cgroup limits for memory restrictions
- GitHub Actions' Ubuntu runners don't properly expose cgroup info
- Elasticsearch attempts JMX reconfiguration during startup
- JMX reconfiguration fails due to null cgroup info
- Container exits with code 1

**Why the fix works**:
- `-Dcom.sun.management.jmxremote=false`: Skips JMX setup entirely
- `-XX:+IgnoreUnrecognizedVMOptions`: Prevents option errors across Java versions
- Safe to use with both ES 7.x and 8.x

**ES 8.13 advantage**:
- Built with Java 17 in mind
- Better cgroup handling in newer ES versions
- Long-term support (7.17 is EOL as of September 2024)
