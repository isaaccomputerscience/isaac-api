/**
 * Copyright 2021 James Sharkey
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

package uk.ac.cam.cl.dtg.isaac.dos.users;

/**
 * An internal class to represent the state of a user that needs synchronising with an external provider.
 */
public class UserExternalAccountChanges {

  private Long userId;
  private String providerUserId;
  private String accountEmail;
  private Role role;
  private String givenName;
  private Boolean deleted;
  private EmailVerificationStatus emailVerificationStatus;
  private Boolean allowsNewsEmails;
  private Boolean allowsEventsEmails;
  private String stage;

  public UserExternalAccountChanges(final Long userId, final String providerUserId, final String accountEmail,
                                    final Role role,
                                    final String givenName, final Boolean deleted,
                                    final EmailVerificationStatus emailVerificationStatus,
                                    final Boolean allowsNewsEmails, final Boolean allowsEventsEmails,
                                    final String stage) {
    this.userId = userId;
    this.providerUserId = providerUserId;
    this.accountEmail = accountEmail;
    this.role = role;
    this.givenName = givenName;
    this.deleted = deleted;
    this.emailVerificationStatus = emailVerificationStatus;
    this.allowsNewsEmails = allowsNewsEmails;
    this.allowsEventsEmails = allowsEventsEmails;
    this.stage = stage;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(final Long userId) {
    this.userId = userId;
  }

  public String getProviderUserId() {
    return providerUserId;
  }

  public void setProviderUserId(final String providerUserId) {
    this.providerUserId = providerUserId;
  }

  public String getAccountEmail() {
    return accountEmail;
  }

  public void setAccountEmail(final String accountEmail) {
    this.accountEmail = accountEmail;
  }

  public Role getRole() {
    return role;
  }

  public void setRole(final Role role) {
    this.role = role;
  }

  public String getGivenName() {
    return givenName;
  }

  public void setGivenName(final String givenName) {
    this.givenName = givenName;
  }

  public Boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(final Boolean deleted) {
    this.deleted = deleted;
  }

  public EmailVerificationStatus getEmailVerificationStatus() {
    return emailVerificationStatus;
  }

  public void setEmailVerificationStatus(final EmailVerificationStatus emailVerificationStatus) {
    this.emailVerificationStatus = emailVerificationStatus;
  }

  public Boolean allowsNewsEmails() {
    return allowsNewsEmails;
  }

  public void setAllowsNewsEmails(final Boolean allowsNewsEmails) {
    this.allowsNewsEmails = allowsNewsEmails;
  }

  public Boolean allowsEventsEmails() {
    return allowsEventsEmails;
  }

  public void setAllowsEventsEmails(final Boolean allowsEventsEmails) {
    this.allowsEventsEmails = allowsEventsEmails;
  }

  public String getStage() {
    return stage;
  }

  public void setStage(final String stage) {
    this.stage = stage;
  }
}
