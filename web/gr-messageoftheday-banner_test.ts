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
import './gr-messageoftheday-banner';
import {GrMessageOfTheDayBanner} from './gr-messageoftheday-banner';
import {PluginApi} from '@gerritcodereview/typescript-api/plugin';
import {assert} from '@open-wc/testing';

suite('gr-messageoftheday-banner tests', () => {
  let element: GrMessageOfTheDayBanner;
  let mockGetResponse: any;

  function queryInShadow<T extends HTMLElement>(
    el: HTMLElement,
    selector: string
  ): T | null {
    return el.shadowRoot?.querySelector<T>(selector) || null;
  }

  async function createElement(): Promise<GrMessageOfTheDayBanner> {
    const el = document.createElement(
      'gr-messageoftheday-banner'
    ) as GrMessageOfTheDayBanner;

    // Set properties BEFORE adding to DOM
    el.plugin = {
      getPluginName: () => 'messageoftheday',
      restApi: () => {
        return {
          get: async () => mockGetResponse,
        };
      },
    } as unknown as PluginApi;

    // Add to DOM
    document.body.appendChild(el);

    await el.updateComplete;
    // Wait for async fetch
    await new Promise(resolve => setTimeout(resolve, 50));
    await el.updateComplete;
    return el;
  }

  teardown(() => {
    if (element && element.parentNode) {
      element.parentNode.removeChild(element);
    }
    // Clear cookies
    document.cookie = 'msg-test123=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
  });

  test('does not render when no message', async () => {
    mockGetResponse = {};
    element = await createElement();

    const container = queryInShadow(element, '#container');
    assert.isNull(container, 'container should not exist');
  });

  test('renders message when available', async () => {
    mockGetResponse = {
      html: '<p>Test message</p>',
      content_id: 'test123',
    };
    element = await createElement();

    const container = queryInShadow(element, '#container');
    assert.isNotNull(container, 'container should exist');

    const message = queryInShadow(element, '#message');
    assert.isNotNull(message, 'message div should exist');
    assert.include(message!.innerHTML, 'Test message');
    
    // Verify HTML is actually rendered (not escaped)
    const paragraph = message!.querySelector('p');
    assert.isNotNull(paragraph, 'HTML should be rendered as DOM elements');
    assert.equal(paragraph!.textContent, 'Test message');
  });

  test('hides banner when dismiss button clicked', async () => {
    mockGetResponse = {
      html: '<p>Test message</p>',
      content_id: 'test123',
    };
    element = await createElement();

    let container = queryInShadow(element, '#container');
    assert.isNotNull(container, 'container should exist initially');

    const dismissBtn = queryInShadow<HTMLElement>(element, '#dismissMessageBtn');
    assert.isNotNull(dismissBtn, 'dismiss button should exist');

    dismissBtn!.click();
    await element.updateComplete;

    container = queryInShadow(element, '#container');
    assert.isNull(container, 'container should be hidden after dismiss');
  });

  test('respects cookie to hide message', async () => {
    // Set cookie before creating element
    document.cookie = 'msg-test123=1; path=/';

    mockGetResponse = {
      html: '<p>Test message</p>',
      content_id: 'test123',
    };
    element = await createElement();

    const container = queryInShadow(element, '#container');
    assert.isNull(container, 'container should not exist when cookie is set');
  });
});
