import { describe, it, expect, vi } from 'vitest';
import { render } from '@testing-library/react';
// import React from 'react';
import { ComponentRenderer } from './ComponentRenderer';
import type { UIComponent } from '../ui-types';

describe('ComponentRenderer', () => {
  it('should render simple div component', () => {
    const component: UIComponent = {
      type: 'div',
      props: {
        children: 'Hello World'
      }
    };

    const { container } = render(
      <ComponentRenderer
        component={component}
        pageId="test"
        context={{}}
      />
    );

    expect(container.textContent).toBe('Hello World');
  });

  it('should resolve template expressions', () => {
    const component: UIComponent = {
      type: 'div',
      props: {
        children: 'Hello ${name}'
      }
    };

    const context = { name: 'World' };
    const { container } = render(
      <ComponentRenderer
        component={component}
        pageId="test"
        context={context}
      />
    );

    expect(container.textContent).toBe('Hello World');
  });

  it('should handle nested components', () => {
    const component: UIComponent = {
      type: 'div',
      className: 'container',
      children: [
        {
          type: 'p',
          props: {
            children: 'Nested content'
          }
        }
      ]
    };

    const { container } = render(
      <ComponentRenderer
        component={component}
        pageId="test"
        context={{}}
      />
    );

    expect(container.querySelector('p')?.textContent).toBe('Nested content');
  });

  it('should handle conditional rendering with hidden prop', () => {
    const component: UIComponent = {
      type: 'div',
      props: {
        hidden: true
      },
      children: [
        {
          type: 'p',
          props: {
            children: 'Should not render'
          }
        }
      ]
    };

    const { container } = render(
      <ComponentRenderer
        component={component}
        pageId="test"
        context={{}}
      />
    );

    expect(container.querySelector('div')).toBeNull();
  });

  it('should handle boolean expression in hidden prop', () => {
    const component: UIComponent = {
      type: 'div',
      props: {
        hidden: "${show === false}",
        children: 'Content'
      }
    };

    const context = { show: false };
    const { container } = render(
      <ComponentRenderer
        component={component}
        pageId="test"
        context={context}
      />
    );

    expect(container.querySelector('div')).toBeNull();
  });

  it('should call onAction when button is clicked', async () => {
    const mockAction = vi.fn().mockResolvedValue({});
    const component: UIComponent = {
      type: 'Button',
      props: {
        onClick: 'testAction',
        children: 'Click me'
      }
    };

    const { getByText } = render(
      <ComponentRenderer
        component={component}
        pageId="test"
        context={{}}
        onAction={mockAction}
      />
    );

    const button = getByText('Click me');
    button.click();

    expect(mockAction).toHaveBeenCalledWith('testAction', {});
  });

  it('should resolve className with template', () => {
    const component: UIComponent = {
      type: 'div',
      className: 'base ${active ? "active" : "inactive"}',
      props: {
        children: 'Test'
      }
    };

    const context = { active: true };
    const { container } = render(
      <ComponentRenderer
        component={component}
        pageId="test"
        context={context}
      />
    );

    const div = container.querySelector('div');
    expect(div?.className).toContain('base');
  });

  it('should prioritize props.children over component.children', () => {
    const component: UIComponent = {
      type: 'div',
      props: {
        children: 'Props children'
      },
      children: [
        {
          type: 'p',
          props: {
            children: 'Component children'
          }
        }
      ]
    };

    const { container } = render(
      <ComponentRenderer
        component={component}
        pageId="test"
        context={{}}
      />
    );

    // Should render props.children, not component.children
    expect(container.textContent).toBe('Props children');
    expect(container.querySelector('p')).toBeNull();
  });

  it('should handle boolean false in hidden prop', () => {
    const component: UIComponent = {
      type: 'div',
      props: {
        hidden: false,
        children: 'Should render'
      }
    };

    const { container } = render(
      <ComponentRenderer
        component={component}
        pageId="test"
        context={{}}
      />
    );

    const div = container.querySelector('div');
    expect(div).not.toBeNull();
    expect(div?.textContent).toBe('Should render');
  });

  it('should handle string literal false in boolean expression', () => {
    const component: UIComponent = {
      type: 'div',
      props: {
        hidden: "${show === false}",
        children: 'Content'
      }
    };

    const context = { show: false };
    const { container } = render(
      <ComponentRenderer
        component={component}
        pageId="test"
        context={context}
      />
    );

    // show === false should evaluate to true, so component should be hidden
    expect(container.querySelector('div')).toBeNull();
  });
});
