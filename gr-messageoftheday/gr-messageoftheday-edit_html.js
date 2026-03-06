/**
 * @license
 * Copyright (C) 2024 The Android Open Source Project
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

export const htmlTemplate = Polymer.html`
  <style include="gr-modal-styles gr-material-styles">
    input, select {
      background-color: var(--select-background-color);
      color: var(--primary-text-color);
      border: 1px solid var(--border-color);
      border-radius: var(--border-radius);
      padding: var(--spacing-s);
      font: inherit;
    }
    gr-autogrow-textarea, #messagePreview {
      background-color: var(--view-background-color);
      color: var(--primary-text-color);
      font: inherit;
      width: 80ch;
      height: 25ch;
      border: 1px solid var(--border-color);
      border-radius: var(--border-radius);
      box-sizing: border-box;
    }
    gr-autogrow-textarea:focus-within {
      border: 2px solid var(--input-focus-border-color);
    }
    #messagePreview {
      background-color: var(--background-color-tertiary);
      overflow-y: auto;
    }
    section {
      margin-bottom: 1em;
    }
    md-icon-button {
      --md-sys-color-on-surface-variant: var(--header-text-color);
    }
    md-icon[filled] {
      font-variation-settings: 'FILL' 1;
    }
    gr-icon {
      --gr-button-text-color: var(--header-text-color);
      color: var(--header-text-color);
    }
    .value {
      display: flex;
      flex-direction: column;
      margin-bottom: 10px;
    }
    .value > * {
      margin: 0;
    }
    .icon-button {
      background: none;
      box-shadow: none;
      padding: 0;
      min-width: 0;
    }

  </style>
  <template is="dom-if" if="[[_can_update]]">
    <md-icon-button class="icon-button" on-click="_openDialog">
      <md-icon filled>campaign</md-icon>
    </md-icon-button>
  </template>
  <dialog id="message_dialog_overlay" tabindex="-1">
    <gr-dialog id="message_dialog" confirm-label="Save Message"
        on-confirm="_saveMessage" on-cancel="_closeDialog">
      <div class="header" slot="header">Set Banner Message</div>
      <div class="main" slot="main">
        <section>
          <span class="title">Expire After:</span>
          <span class="value">
            <div style="display: flex; align-items: center; gap: 5px;">
              <md-outlined-text-field
                id="expireAfterInput"
                class="showBlueFocusBorder"
                placeholder="Enter Number"
                value="[[_expire_after_value]]"
                on-input="_onExpireAfterValueInput">
              </md-outlined-text-field>
              <md-outlined-select
                id="expireAfterUnitSelect"
                value="[[_expire_after_unit]]"
                on-change="_onExpireAfterUnitChange">
                <md-select-option value="m">
                  <div slot="headline">minutes</div>
                </md-select-option>
                <md-select-option value="h">
                  <div slot="headline">hours</div>
                </md-select-option>
                <md-select-option value="d">
                  <div slot="headline">days</div>
                </md-select-option>
                <md-select-option value="w">
                  <div slot="headline">weeks</div>
                </md-select-option>
              </md-outlined-select>
            </div>
          </span>
        </section>
        <section>
          <span class="title">Message:</span>
          <span class="value">
            <gr-autogrow-textarea
                class="text_area"
                placeholder="Enter Message"
                autocomplete="off"
                value="[[_message]]"
                on-input="_onMessageInput"/>
          </span>
        </section>
        <section>
          <span class="title">Preview:</span>
          <span class="value">
            <div id="messagePreview" readonly>{{_message}}</div>
          </span>
        </section>
      </div>
    </gr-dialog>
  </dialog>
`;
