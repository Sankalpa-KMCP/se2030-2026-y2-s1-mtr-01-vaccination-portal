import React, { useEffect, useRef, useState } from 'react';
import HeroArtwork from './HeroArtwork';
import { disposeSceneResources } from './heroThreeUtils';

const REDUCED_MOTION_QUERY = '(prefers-reduced-motion: reduce)';
const MAX_PARALLAX = Math.PI / 60;

const HERO_LABELS = [
  { label: 'Campaign', className: 'home-care-scene__label--campaign' },
  { label: 'Eligibility', className: 'home-care-scene__label--eligibility' },
  { label: 'Appointment', className: 'home-care-scene__label--appointment' },
  { label: 'Dose', className: 'home-care-scene__label--dose' },
  { label: 'Record', className: 'home-care-scene__label--record' },
];

function supportsWebGL(canvas) {
  try {
    if (!window.WebGLRenderingContext && !window.WebGL2RenderingContext) return false;
    return Boolean(
      canvas?.getContext?.('webgl2', { alpha: true, antialias: true })
      || canvas?.getContext?.('webgl', { alpha: true, antialias: true }),
    );
  } catch {
    return false;
  }
}

export default function HeroThreeScene() {
  const hostRef = useRef(null);
  const canvasRef = useRef(null);
  const [sceneState, setSceneState] = useState('loading');

  useEffect(() => {
    const host = hostRef.current;
    const canvas = canvasRef.current;
    if (!host || !canvas || typeof window === 'undefined') return undefined;

    let cancelled = false;
    let frameId = null;
    let renderer = null;
    let scene = null;
    let constellation = null;
    let resizeObserver = null;
    let visibilityObserver = null;
    let isVisible = true;
    let elapsed = 0;
    let previousTime = 0;
    const pointerTarget = { x: 0, y: 0 };
    const pointerCurrent = { x: 0, y: 0 };

    const stopRendering = () => {
      if (frameId !== null) window.cancelAnimationFrame(frameId);
      frameId = null;
    };

    const handlePointerMove = (event) => {
      const bounds = host.getBoundingClientRect();
      pointerTarget.x = ((event.clientX - bounds.left) / Math.max(bounds.width, 1) - 0.5) * 2;
      pointerTarget.y = ((event.clientY - bounds.top) / Math.max(bounds.height, 1) - 0.5) * 2;
    };

    const handlePointerLeave = () => {
      pointerTarget.x = 0;
      pointerTarget.y = 0;
    };

    const handleContextLost = (event) => {
      event.preventDefault();
      stopRendering();
      setSceneState('fallback');
    };

    canvas.addEventListener('webglcontextlost', handleContextLost);

    const motionPreference = typeof window.matchMedia === 'function'
      ? window.matchMedia(REDUCED_MOTION_QUERY)
      : null;
    if (motionPreference?.matches || !supportsWebGL(canvas)) {
      setSceneState('fallback');
      return () => canvas.removeEventListener('webglcontextlost', handleContextLost);
    }

    async function initialise() {
      try {
        const THREE = await import('three');
        if (cancelled) return;

        const context = canvas.getContext('webgl2', { alpha: true, antialias: true })
          || canvas.getContext('webgl', { alpha: true, antialias: true });
        if (!context) return;

        scene = new THREE.Scene();
        const camera = new THREE.PerspectiveCamera(35, 1, 0.1, 100);
        camera.position.set(0, 0.35, 9.8);

        renderer = new THREE.WebGLRenderer({ canvas, context, alpha: true, antialias: true });
        renderer.setClearColor(0x000000, 0);
        renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 1.5));
        renderer.outputColorSpace = THREE.SRGBColorSpace;
        renderer.toneMapping = THREE.ACESFilmicToneMapping;
        renderer.toneMappingExposure = 1.04;

        constellation = new THREE.Group();
        constellation.rotation.x = -0.08;
        scene.add(constellation);

        const coreMaterial = new THREE.MeshPhysicalMaterial({
          color: 0xe8e2f0,
          roughness: 0.18,
          metalness: 0.04,
          transmission: 0.46,
          thickness: 1.2,
          transparent: true,
          opacity: 0.92,
          clearcoat: 1,
          clearcoatRoughness: 0.16,
        });
        const matteMaterial = new THREE.MeshStandardMaterial({
          color: 0x8b80a8,
          roughness: 0.54,
          metalness: 0.08,
        });
        const pearlMaterial = new THREE.MeshPhysicalMaterial({
          color: 0xfcfbf8,
          roughness: 0.28,
          metalness: 0.02,
          transmission: 0.18,
          transparent: true,
          opacity: 0.96,
          clearcoat: 0.7,
        });
        const champagneMaterial = new THREE.MeshStandardMaterial({
          color: 0xb8a27c,
          roughness: 0.38,
          metalness: 0.56,
        });

        const core = new THREE.Mesh(new THREE.IcosahedronGeometry(1.3, 5), coreMaterial);
        core.scale.set(1, 1.05, 0.82);
        constellation.add(core);

        const halo = new THREE.Mesh(
          new THREE.TorusGeometry(1.72, 0.026, 12, 128),
          champagneMaterial,
        );
        halo.rotation.set(1.18, 0.12, 0.35);
        constellation.add(halo);

        const positions = [
          [-2.65, 1.2, 0.15],
          [-2.15, -1.35, 0.45],
          [0.05, -2.05, -0.05],
          [2.2, -1.15, 0.35],
          [2.62, 1.16, 0.05],
        ];
        const forms = [
          new THREE.CapsuleGeometry(0.42, 0.72, 8, 18),
          new THREE.IcosahedronGeometry(0.68, 2),
          new THREE.BoxGeometry(1.05, 0.78, 0.72, 4, 4, 4),
          new THREE.CylinderGeometry(0.54, 0.54, 0.92, 32),
          new THREE.SphereGeometry(0.63, 32, 20),
        ];

        const nodes = forms.map((geometry, index) => {
          const material = index % 2 === 0 ? matteMaterial.clone() : pearlMaterial.clone();
          if (index === 2) material.color.setHex(0xb8a27c);
          const node = new THREE.Mesh(geometry, material);
          node.position.set(...positions[index]);
          node.rotation.set(index * 0.28, index * -0.34, index * 0.18);
          constellation.add(node);
          return node;
        });
        matteMaterial.dispose();
        pearlMaterial.dispose();

        const routePoints = positions.map(([x, y, z]) => new THREE.Vector3(x, y, z - 0.35));
        routePoints.push(new THREE.Vector3(-2.65, 1.2, -0.2));
        const route = new THREE.CatmullRomCurve3(routePoints, true, 'catmullrom', 0.35);
        const routeMesh = new THREE.Mesh(
          new THREE.TubeGeometry(route, 180, 0.018, 8, true),
          new THREE.MeshStandardMaterial({ color: 0xb8a27c, roughness: 0.42, metalness: 0.55 }),
        );
        constellation.add(routeMesh);

        const shadow = new THREE.Mesh(
          new THREE.CircleGeometry(3.55, 64),
          new THREE.MeshBasicMaterial({ color: 0x4e465f, transparent: true, opacity: 0.08, depthWrite: false }),
        );
        shadow.scale.set(1.25, 0.34, 1);
        shadow.position.set(0, -2.55, -1.35);
        constellation.add(shadow);

        scene.add(new THREE.HemisphereLight(0xfefcf8, 0x6d647f, 2.25));
        const keyLight = new THREE.DirectionalLight(0xfff7ea, 3.2);
        keyLight.position.set(-4, 5, 7);
        scene.add(keyLight);
        const lavenderLight = new THREE.PointLight(0xa697c7, 18, 12);
        lavenderLight.position.set(4, 1, 4);
        scene.add(lavenderLight);

        const resize = () => {
          if (!renderer) return;
          const width = Math.max(host.clientWidth, 1);
          const height = Math.max(host.clientHeight, 1);
          renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 1.5));
          renderer.setSize(width, height, false);
          camera.aspect = width / height;
          camera.updateProjectionMatrix();
        };

        const renderFrame = (time) => {
          if (cancelled || !isVisible || !renderer) {
            frameId = null;
            return;
          }
          const delta = Math.min((time - previousTime) / 1000 || 0, 0.05);
          previousTime = time;
          elapsed += delta;
          pointerCurrent.x += (pointerTarget.x - pointerCurrent.x) * 0.045;
          pointerCurrent.y += (pointerTarget.y - pointerCurrent.y) * 0.045;
          constellation.rotation.y = Math.sin(elapsed * 0.18) * 0.035 + pointerCurrent.x * MAX_PARALLAX;
          constellation.rotation.x = -0.08 - pointerCurrent.y * MAX_PARALLAX;
          constellation.position.y = Math.sin(elapsed * 0.5) * 0.07;
          core.rotation.y += delta * 0.09;
          halo.rotation.z += delta * 0.035;
          nodes.forEach((node, index) => {
            node.position.y = positions[index][1] + Math.sin(elapsed * 0.62 + index) * 0.045;
            node.rotation.y += delta * (0.05 + index * 0.008);
          });
          renderer.render(scene, camera);
          frameId = window.requestAnimationFrame(renderFrame);
        };

        const startRendering = () => {
          if (frameId === null && !cancelled && isVisible) {
            previousTime = performance.now();
            frameId = window.requestAnimationFrame(renderFrame);
          }
        };

        resizeObserver = typeof ResizeObserver === 'function' ? new ResizeObserver(resize) : null;
        resizeObserver?.observe(host);
        visibilityObserver = typeof IntersectionObserver === 'function'
          ? new IntersectionObserver(([entry]) => {
            isVisible = entry.isIntersecting;
            if (isVisible) startRendering(); else stopRendering();
          }, { rootMargin: '120px' })
          : null;
        visibilityObserver?.observe(host);
        host.addEventListener('pointermove', handlePointerMove, { passive: true });
        host.addEventListener('pointerleave', handlePointerLeave);
        resize();
        renderer.render(scene, camera);
        setSceneState('ready');
        startRendering();
      } catch {
        setSceneState('fallback');
      }
    }

    initialise();

    return () => {
      cancelled = true;
      stopRendering();
      resizeObserver?.disconnect();
      visibilityObserver?.disconnect();
      host.removeEventListener('pointermove', handlePointerMove);
      host.removeEventListener('pointerleave', handlePointerLeave);
      canvas.removeEventListener('webglcontextlost', handleContextLost);
      disposeSceneResources(scene);
      renderer?.dispose();
    };
  }, []);

  return (
    <div className="home-care-scene" data-scene-state={sceneState} ref={hostRef}>
      <div className="home-care-scene__fallback" aria-hidden="true">
        <HeroArtwork />
      </div>
      <canvas className="home-care-scene__canvas" ref={canvasRef} aria-hidden="true" />
      <ol className="home-care-scene__labels" aria-label="Care pathway stages">
        {HERO_LABELS.map((item, index) => (
          <li className={`home-care-scene__label ${item.className}`} key={item.label}>
            <i>{String(index + 1).padStart(2, '0')}</i>
            {item.label}
          </li>
        ))}
      </ol>
      <span className="home-care-scene__core-label" aria-hidden="true">Care core</span>
    </div>
  );
}
