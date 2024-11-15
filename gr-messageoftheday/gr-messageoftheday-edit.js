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
        observer: '_messageChanged'
      },
      _expire_after_value: {
        type: String,
      },
      _expire_after_unit: {
        type: String,
      },
      _can_update: {
        type: Boolean,
        value: false,
      },
      _show_update_banner: {
        type: Boolean,
        value: false,
      },
    };
  }

  connectedCallback() {
    super.connectedCallback();
  }

  ready() {
    super.ready();
    this._canUpdate();
  }

  _canUpdate() {
    const endpoint = `/accounts/self/capabilities?q=messageoftheday-updateBanner`;
    return this.plugin.restApi().get(endpoint).then(response => {
      if (response && response['messageoftheday-updateBanner'] === true) {
        this._can_update = true;
        this._fetchMessage();
      }
    }).catch(error => {
      console.error('Error checking updateBanner capability:', error);
      this._can_update = false;
    });
  }

  _fetchMessage() {
    return this.plugin.restApi().get("/config/server/messageoftheday~message").then(response => {
      if (response) {
        this._message = response.html;
      } else {
        this._message = '';
      }
    }).catch(error => {
      console.error('Error fetching message:', error);
      this._message = '';
    });
  }

  _saveMessage() {
    const endpoint = `/config/server/messageoftheday~message`;
    const payload = {
      message: this._message
    };
    if (this._expire_after_value) {
      payload.expires_at = this._convertToFormattedDate(
          this._expire_after_value, this._expire_after_unit);
    }

    return this.plugin.restApi().post(endpoint, payload).then(
        response => {
          location.reload();
        }
    ).catch(error => {
      console.error('Error saving message:', error);
    });
  }

  _openDialog() {
    this.$.message_dialog_overlay.show();
    this.$.message_dialog.classList.toggle('invisible', false);
  }

  _closeDialog() {
    this.$.message_dialog.classList.toggle('invisible', true);
    this.$.message_dialog_overlay.close();
  }

  _messageChanged(newMessage) {
    const messagePreview = this.shadowRoot.querySelector('#messagePreview');
    if (messagePreview) {
      messagePreview.innerHTML = newMessage;
    }
  }

  _convertToFormattedDate(value, unit) {
    let msToAdd = 0;
    switch (unit) {
      case 'm':
        msToAdd = value * 60 * 1000;
        break;
      case 'h':
        msToAdd = value * 60 * 60 * 1000;
        break;
      case 'd':
        msToAdd = value * 24 * 60 * 60 * 1000;
        break;
      case 'w':
        msToAdd = value * 7 * 24 * 60 * 60 * 1000;
        break;
    }

    const now = new Date();
    const future = new Date(now.getTime() + msToAdd);
    const options = {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      timeZoneName: 'short'
    };
    return new Intl.DateTimeFormat('en-US', options).format(future);
  }
}

customElements.define(GrMessageOfTheDayEdit.is, GrMessageOfTheDayEdit);
