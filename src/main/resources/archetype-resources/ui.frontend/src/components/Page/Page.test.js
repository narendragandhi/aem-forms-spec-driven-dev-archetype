/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe Systems Incorporated
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

import { ModelManager } from '@adobe/aem-spa-page-model-manager';
import React from 'react';
import { render, cleanup } from '@testing-library/react';
import sinon from 'sinon';
import Page from './Page';

describe('Page ->', () => {
  const PAGE_CLASS_NAME = 'page';

  let sandbox = sinon.createSandbox();

  beforeEach(() => {
    ModelManager.initialize();
  });

  afterEach(() => {
    window.location.hash = '';
    cleanup();
    sandbox.restore();
  });

  it('should render the page component with no parameter', () => {
    const { container } = render(<Page />);

    expect(container.childElementCount).toEqual(1);

    expect(container.querySelector('.' + PAGE_CLASS_NAME)).not.toBeNull();
  });

  it('should render the page component with extra class names', () => {
    const EXTRA_CLASS_NAMES = 'test-class-names';

    const { container } = render(<Page cssClassNames={EXTRA_CLASS_NAMES} />);

    expect(container.childElementCount).toEqual(1);

    expect(container.querySelector('.' + EXTRA_CLASS_NAMES)).not.toBeNull();
  });
});
