/*
 * Copyright 2014 JBoss, by Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.workbench.common.services.refactoring.model.index;

public enum IndexableElements {

    RULE_NAME,
    RULE_NAME_PARENT,
    RULE_ATTRIBUTE_NAME,
    RULE_ATTRIBUTE_VALUE,
    TYPE_NAME,
    FIELD_TYPE_NAME,
    FIELD_TYPE_FULLY_QUALIFIED_CLASS_NAME;

    @Override
    public String toString() {
        return super.name().toLowerCase();
    }
}
