/**
 * @license
 * Copyright (C) 2021 The Android Open Source Project
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
import {htmlTemplate} from './gr-messageoftheday-banner_html.js';

class GrMessageOfTheDayBanner extends Polymer.Element {
  static get is() {
    return 'gr-messageoftheday-banner';
  }

  static get template() {
    return htmlTemplate;
  }

  static get properties() {
    return {
      _message: Object,
      _hidden: {
        type: Boolean,
        value: true,
      }
    };
  }

  connectedCallback() {
    super.connectedCallback();

    this.plugin.restApi()
      .get(`/config/server/${this.plugin.getPluginName()}~message`)
      .then(message => {
        if (!message || !message.html) {
          return;
        }
        this._message = message;
        this._isHidden();
        this.$.message.innerHTML = this._message.html;
      });
  }

  _handleDismissMessage() {
    document.cookie =
      `msg-${this._message.id}=1; path=/; expires=${this._message.redisplay}`;
    this._hidden = true;
  }

  _isHidden() {
    this._hidden = document.cookie.search(`msg-${this._message.id}=`) > -1;
  }
}

customElements.define(GrMessageOfTheDayBanner.is, GrMessageOfTheDayBanner);
