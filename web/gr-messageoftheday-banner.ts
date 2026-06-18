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
    'gr-messageoftheday-banner': GrMessageOfTheDayBanner;
  }
}

interface Message {
  html?: string;
  content_id?: string;
}

@customElement('gr-messageoftheday-banner')
export class GrMessageOfTheDayBanner extends LitElement {
  @query('#message')
  messageElement?: HTMLDivElement;

  @property({ type: Object })
  plugin!: PluginApi;

  @state()
  private message?: Message;

  @state()
  private isHidden = true;

  static override get styles() {
    return [
      window.Gerrit?.styles.font as CSSResult,
      css`
        #container {
          background-color: var(--line-item-highlight-color);
          display: flex;
          height: fit-content;
          justify-content: space-between;
          align-items: center;
          padding: 1em;
        }
        #message {
          flex-grow: 1;
        }
      `,
    ];
  }

  override connectedCallback() {
    super.connectedCallback();
    this.fetchMessage();
  }

  private async fetchMessage() {
    this.plugin
        .restApi()
        .get<Message>(
          `/config/server/${this.plugin.getPluginName()}~message`
        )
      .then(message => {
        if (!message || !message.html) {
          return;
        }

        this.message = message;
        this.checkIsHidden();

        // Wait for render then set innerHTML
        this.updateComplete.then(() => {
          if (this.messageElement && this.message?.html) {
            this.messageElement.innerHTML = this.message.html;
          }
        })

      })
      .catch(error => {
        console.error('Error fetching message:', error);
      })
  }

  override render() {
    if (this.isHidden) {
      return html``;
    }

    return html`
      <div id="container">
        <div id="message"></div>
        <gr-button id="dismissMessageBtn" link @click="${this.handleDismiss}">
          Dismiss
        </gr-button>
      </div>
    `;
  }

  private handleDismiss() {
    if (!this.message?.content_id) return;

    document.cookie = `msg-${this.message.content_id}=1; path=/; expires=${this.getExpires()}`;
    this.isHidden = true;
  }

  private checkIsHidden() {
    if (!this.message?.content_id) {
      this.isHidden = true;
      return;
    }
    this.isHidden =
      document.cookie.search(`msg-${this.message.content_id}=`) > -1;
  }

  private getExpires(): string {
    const date = new Date();
    date.setHours(0, 0, 0, 0);
    date.setDate(date.getDate() + 1);
    return date.toUTCString();
  }
}
