/**
 * Copyright (C) 2012 Stephan Classen
 * Based on guice-perist (Copyright (C) 2010 Google, Inc.)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.sclassen.guicejpa.testframework;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Entities which can be created during transaction tests.
 * The ID will be unique in every run of a test.
 *
 * @author Stephan Classen
 */
@Entity
public final class TransactionTestEntity implements Serializable {

  private static final long serialVersionUID = 1L;

  private UUID id = UUID.randomUUID();
  private String text;

  /**
   * Default constructor.
   */
  public TransactionTestEntity() {
    // nop
  }

  /**
   * Convenience constructor for directly setting the text.
   *
   * @param text
   *            will be stored in the column {@code text}.
   */
  public TransactionTestEntity(String text) {
    this.text = text;
  }

  @Id
  public UUID getId() {
    return id;
  }

  @SuppressWarnings("unused")
  // used by the persistence framework
  private void setId(UUID id) {
    this.id = id;
  }

  @Column(length = 200)
  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

}
