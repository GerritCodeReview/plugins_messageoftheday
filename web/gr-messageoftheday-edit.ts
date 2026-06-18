/**
 * @license
 * Copyright (C) 2026 The Android Open Source Project
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
import { PluginApi } from '@gerritcodereview/typescript-api/plugin';
import { css, CSSResult, html, LitElement } from 'lit';
import { customElement, property, query, state } from 'lit/decorators.js';

declare global {
  interface HTMLElementTagNameMap {
    'gr-messageoftheday-edit': GrMessageOfTheDayEdit;
  }
}

interface MessageResponse {
  html?: string;
}

interface CapabilitiesResponse {
  'messageoftheday-updateBanner'?: boolean;
}

type ExpireUnit = 'm' | 'h' | 'd' | 'w';

@customElement('gr-messageoftheday-edit')
export class GrMessageOfTheDayEdit extends LitElement {
  @query('#message_dialog_overlay')
  dialogOverlay?: HTMLDialogElement;

  @query('#message_dialog')
  dialog?: HTMLElement;

  @query('#messagePreview')
  messagePreview?: HTMLDivElement;

  @property({ type: Object })
  plugin!: PluginApi;

  @state()
  private message = '';

  @state()
  private expireAfterValue = '';

  @state()
  private expireAfterUnit: ExpireUnit = 'd';

  @state()
  private canUpdate = false;

  static override get styles() {
    return [
      window.Gerrit?.styles.font as CSSResult,
      window.Gerrit?.styles.form as CSSResult,
      window.Gerrit?.styles.modal as CSSResult,
      css`
        input,
        select {
          background-color: var(--select-background-color);
          color: var(--primary-text-color);
          border: 1px solid var(--border-color);
          border-radius: var(--border-radius);
          padding: var(--spacing-s);
          font: inherit;
        }
        gr-autogrow-textarea,
        #messagePreview {
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
          padding: var(--spacing-s);
        }
        section {
          margin-bottom: 1em;
        }
        .icon-button {
          background: none;
          box-shadow: none;
          padding: 0;
          min-width: 0;
          border: none;
          cursor: pointer;
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
        .expire-inputs {
          display: flex;
          align-items: center;
          gap: 5px;
        }
      `,
    ];
  }

  override connectedCallback() {
    super.connectedCallback();
    this.checkCanUpdate();
  }

  private async checkCanUpdate() {
    this.plugin
      .restApi()
      .get<CapabilitiesResponse>(
        '/accounts/self/capabilities?q=messageoftheday-updateBanner'
      )
      .then(response => {
        if (response && response['messageoftheday-updateBanner'] === true) {
          this.canUpdate = true;
          this.fetchMessage();
        }
      })
      .catch(error => {
        console.error('Error checking updateBanner capability:', error);
        this.canUpdate = false;
      })
  }

  private async fetchMessage() {
    this.plugin
      .restApi()
      .get<MessageResponse>('/config/server/messageoftheday~message')
      .then(response => {
        this.message = response?.html || '';
      })
      .catch(error => {
        console.error('Error fetching message:', error);
        this.message = '';
      })
  }

  override render() {
    if (!this.canUpdate) {
      return html``;
    }

    return html`
      <button class="icon-button" @click="${this.openDialog}">
        📢
      </button>
      <dialog id="message_dialog_overlay" tabindex="-1">
        <gr-dialog
          id="message_dialog"
          confirm-label="Save Message"
          @confirm="${this.saveMessage}"
          @cancel="${this.closeDialog}"
        >
          <div class="header" slot="header">Set Banner Message</div>
          <div class="main" slot="main">
            <section>
              <span class="title">Expire After:</span>
              <span class="value">
                <div class="expire-inputs">
                  <input
                    id="expireAfterInput"
                    type="number"
                    placeholder="Enter Number"
                    .value="${this.expireAfterValue}"
                    @input="${this.handleExpireValueInput}"
                  />
                  <select
                    id="expireAfterUnitSelect"
                    .value="${this.expireAfterUnit}"
                    @change="${this.handleExpireUnitChange}"
                  >
                    <option value="m">minutes</option>
                    <option value="h">hours</option>
                    <option value="d" ?selected="${this.expireAfterUnit === 'd'}">
                      days
                    </option>
                    <option value="w">weeks</option>
                  </select>
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
                  .value="${this.message}"
                  @input="${this.handleMessageInput}"
                ></gr-autogrow-textarea>
              </span>
            </section>
            <section>
              <span class="title">Preview:</span>
              <span class="value">
                <div id="messagePreview" readonly></div>
              </span>
            </section>
          </div>
        </gr-dialog>
      </dialog>
    `;
  }

  override updated(changedProperties: Map<string, any>) {
    if (changedProperties.has('message') && this.messagePreview) {
      this.messagePreview.innerHTML = this.message;
    }
  }

  override firstUpdated() {
    // Set initial preview content if message exists
    if (this.messagePreview && this.message) {
      this.messagePreview.innerHTML = this.message;
    }
  }

  private openDialog() {
    this.dialogOverlay?.showModal();
    this.dialog?.classList.toggle('invisible', false);
  }

  private closeDialog() {
    this.dialog?.classList.toggle('invisible', true);
    this.dialogOverlay?.close();
  }

  private handleMessageInput(e: CustomEvent) {
    this.message = e.detail.value;
  }

  private handleExpireValueInput(e: Event) {
    const target = e.target as HTMLInputElement;
    this.expireAfterValue = target.value || '';
  }

  private handleExpireUnitChange(e: Event) {
    const target = e.target as HTMLSelectElement;
    this.expireAfterUnit = (target.value as ExpireUnit) || 'd';
  }

  private async saveMessage() {
    const payload: { message: string; expires_at?: string } = {
      message: this.message,
    };

    if (this.expireAfterValue) {
      payload.expires_at = this.convertToFormattedDate(
        this.expireAfterValue,
        this.expireAfterUnit
      );
    }

    try {
      await this.plugin
        .restApi()
        .post('/config/server/messageoftheday~message', payload);
      location.reload();
    } catch (error) {
      console.error('Error saving message:', error);
    }
  }

  private convertToFormattedDate(value: string, unit: ExpireUnit): string {
    const numValue = parseInt(value, 10);
    let msToAdd = 0;

    switch (unit) {
      case 'm':
        msToAdd = numValue * 60 * 1000;
        break;
      case 'h':
        msToAdd = numValue * 60 * 60 * 1000;
        break;
      case 'd':
        msToAdd = numValue * 24 * 60 * 60 * 1000;
        break;
      case 'w':
        msToAdd = numValue * 7 * 24 * 60 * 60 * 1000;
        break;
    }

    const now = new Date();
    const future = new Date(now.getTime() + msToAdd);
    const options: Intl.DateTimeFormatOptions = {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      timeZoneName: 'short',
    };
    return new Intl.DateTimeFormat('en-US', options).format(future);
  }
}
