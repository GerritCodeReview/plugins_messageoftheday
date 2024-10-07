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
import {htmlTemplate} from './gr-messageoftheday-edit_html.js';

class GrMessageOfTheDayEdit extends Polymer.Element {
  static get is() {
    return 'gr-messageoftheday-edit';
  }

  static get template() {
    return htmlTemplate;
  }

  static get properties() {
    return {
      _message: {
        type: String,
      },
    };
  }

  connectedCallback() {
    super.connectedCallback();
  }

  ready() {
    super.ready();
    this._fetchMessage();
  }

  _fetchMessage() {
    return this.plugin.restApi().get("/config/server/messageoftheday~message").then(response => {
      if (response) {
        this._message = response.html;
      } else {
        this._message = '';
      }
    });
  }

  _saveMessage() {
    const endpoint = `/config/server/messageoftheday~message`;
    return this.plugin.restApi().post(endpoint,
        {message: this._message}).then(
        response => {
          location.reload();
        }
    );
  }

  _onKeyPressListener(e) {
    e.stopPropagation();
  }
}

customElements.define(GrMessageOfTheDayEdit.is, GrMessageOfTheDayEdit);
