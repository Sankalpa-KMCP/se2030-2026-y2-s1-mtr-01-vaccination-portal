import React from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, render, screen } from '@testing-library/react';
import HeroThreeScene from './HeroThreeScene';
import { disposeSceneResources } from './heroThreeUtils';

describe('HeroThreeScene', () => {
  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it('keeps meaningful HTML labels and the static artwork when WebGL is unavailable', () => {
    vi.stubGlobal('matchMedia', vi.fn(() => ({ matches: false })));
    vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(null);

    const { container } = render(<HeroThreeScene />);

    expect(container.querySelector('[data-scene-state="fallback"]')).toBeTruthy();
    expect(container.querySelector('.home-pathway')).toBeTruthy();
    expect(screen.getByRole('list', { name: /care pathway stages/i })).toBeTruthy();
    ['Campaign', 'Eligibility', 'Appointment', 'Dose', 'Record'].forEach((label) => {
      expect(screen.getByText(label)).toBeTruthy();
    });
  });

  it('uses the permanent static fallback when reduced motion is requested', () => {
    const getContext = vi.spyOn(HTMLCanvasElement.prototype, 'getContext');
    vi.stubGlobal('matchMedia', vi.fn(() => ({ matches: true })));

    const { container } = render(<HeroThreeScene />);

    expect(container.querySelector('[data-scene-state="fallback"]')).toBeTruthy();
    expect(getContext).not.toHaveBeenCalled();
  });

  it('switches back to the static composition if the WebGL context is lost', () => {
    vi.stubGlobal('matchMedia', vi.fn(() => ({ matches: false })));
    vi.stubGlobal('WebGLRenderingContext', vi.fn());
    vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue({});
    const { container } = render(<HeroThreeScene />);
    const canvas = container.querySelector('canvas');

    act(() => {
      canvas.dispatchEvent(new Event('webglcontextlost', { cancelable: true }));
    });

    expect(container.querySelector('[data-scene-state="fallback"]')).toBeTruthy();
    expect(container.querySelector('.home-pathway')).toBeTruthy();
  });

  it('disposes geometries, materials, and material textures', () => {
    const geometryDispose = vi.fn();
    const materialDispose = vi.fn();
    const textureDispose = vi.fn();
    const root = {
      traverse(callback) {
        callback({
          geometry: { dispose: geometryDispose },
          material: { map: { isTexture: true, dispose: textureDispose }, dispose: materialDispose },
        });
      },
    };

    disposeSceneResources(root);

    expect(geometryDispose).toHaveBeenCalledOnce();
    expect(materialDispose).toHaveBeenCalledOnce();
    expect(textureDispose).toHaveBeenCalledOnce();
  });
});
