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
import './test/test-setup';
import './gr-messageoftheday-edit';
import {GrMessageOfTheDayEdit} from './gr-messageoftheday-edit';
import {PluginApi} from '@gerritcodereview/typescript-api/plugin';
import {assert} from '@open-wc/testing';

suite('gr-messageoftheday-edit tests', () => {
  let element: GrMessageOfTheDayEdit;
  let mockCapabilities: any;
  let mockMessage: any;

  function queryInShadow<T extends HTMLElement>(
    el: HTMLElement,
    selector: string
  ): T | null {
    return el.shadowRoot?.querySelector<T>(selector) || null;
  }

  async function createElement(): Promise<GrMessageOfTheDayEdit> {
    const el = document.createElement(
      'gr-messageoftheday-edit'
    ) as GrMessageOfTheDayEdit;

    // Set properties BEFORE adding to DOM
    el.plugin = {
      restApi: () => {
        return {
          get: async (url: string) => {
            if (url.includes('capabilities')) {
              return mockCapabilities;
            }
            return mockMessage;
          },
          post: async () => ({}),
        };
      },
    } as unknown as PluginApi;

    // Add to DOM
    document.body.appendChild(el);

    await el.updateComplete;
    // Wait for async capability check and message fetch
    await new Promise(resolve => setTimeout(resolve, 100));
    await el.updateComplete;
    return el;
  }

  teardown(() => {
    if (element && element.parentNode) {
      element.parentNode.removeChild(element);
    }
  });

  test('does not render when user lacks capability', async () => {
    mockCapabilities = {};
    mockMessage = {};

    element = await createElement();

    const button = queryInShadow(element, '.icon-button');
    assert.isNull(button, 'button should not exist without capability');
  });

  test('renders button when user has capability', async () => {
    mockCapabilities = {'messageoftheday-updateBanner': true};
    mockMessage = {html: 'Existing message'};

    element = await createElement();

    const button = queryInShadow(element, '.icon-button');
    assert.isNotNull(button, 'button should exist with capability');
  });

  test('preview shows HTML content when message is edited', async () => {
    mockCapabilities = {'messageoftheday-updateBanner': true};
    mockMessage = {html: '<b>Bold text</b>'};

    element = await createElement();

    // Open dialog
    const button = queryInShadow<HTMLElement>(element, '.icon-button');
    button!.click();
    await element.updateComplete;

    const preview = queryInShadow<HTMLDivElement>(element, '#messagePreview');
    assert.isNotNull(preview, 'preview should exist');

    // Check that initial HTML is rendered
    assert.include(preview!.innerHTML, '<b>Bold text</b>');

    // Verify HTML is actually rendered as DOM
    const bold = preview!.querySelector('b');
    assert.isNotNull(bold, 'HTML should be rendered as DOM elements');
    assert.equal(bold!.textContent, 'Bold text');

    // Simulate typing in textarea
    const textarea = queryInShadow(element, 'gr-autogrow-textarea');
    assert.isNotNull(textarea, 'textarea should exist');

    // Simulate input event (like user typing)
    textarea!.dispatchEvent(new CustomEvent('input', {
      detail: {value: '<p>New <em>message</em></p>'},
      bubbles: true,
      composed: true
    }));
    await element.updateComplete;

    // Check preview updates
    assert.include(preview!.innerHTML, '<em>message</em>');
    const em = preview!.querySelector('em');
    assert.isNotNull(em, 'Updated HTML should render');
    assert.equal(em!.textContent, 'message');
  });
});
