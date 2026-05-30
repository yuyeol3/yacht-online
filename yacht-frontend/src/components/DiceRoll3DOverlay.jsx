import { Canvas, useFrame } from "@react-three/fiber";
import { Environment } from "@react-three/drei";
import { useMemo, useRef } from "react";

function targetRotationForValue(value) {
  switch (value) {
    case 1: return { x: 0, y: 0, z: 0 };
    case 2: return { x: 0, y: -Math.PI / 2, z: 0 };
    case 3: return { x: Math.PI / 2, y: 0, z: 0 };
    case 4: return { x: -Math.PI / 2, y: 0, z: 0 };
    case 5: return { x: 0, y: Math.PI / 2, z: 0 };
    case 6: return { x: Math.PI, y: 0, z: 0 };
    default: return { x: 0, y: 0, z: 0 };
  }
}

function pipPattern(value) {
  const a = 0.22;
  switch (value) {
    case 1: return [[0, 0]];
    case 2: return [[-a, a], [a, -a]];
    case 3: return [[-a, a], [0, 0], [a, -a]];
    case 4: return [[-a, a], [a, a], [-a, -a], [a, -a]];
    case 5: return [[-a, a], [a, a], [0, 0], [-a, -a], [a, -a]];
    case 6: return [[-a, a], [-a, 0], [-a, -a], [a, a], [a, 0], [a, -a]];
    default: return [];
  }
}

function FacePips({ value, rotation }) {
  const coords = useMemo(() => pipPattern(value), [value]);
  return (
    <group rotation={rotation}>
      {coords.map(([x, y], idx) => (
        <mesh key={`${value}-${idx}`} position={[x, y, 0.515]}>
          <sphereGeometry args={[0.055, 16, 16]} />
          <meshStandardMaterial color="#0f172a" roughness={0.28} metalness={0.05} />
        </mesh>
      ))}
    </group>
  );
}

function RollingDie({ value, position, order }) {
  const meshRef = useRef(null);
  const startedAt = useRef(null);
  const duration = 1.5;

  const motion = useMemo(() => {
    const target = targetRotationForValue(value);
    const spinX = (order % 2 === 0 ? 4 : 3) * Math.PI;
    const spinY = (order % 2 === 0 ? 3 : 4) * Math.PI;
    const spinZ = (order % 3 === 0 ? 2 : 3) * Math.PI;

    return {
      start: { x: 0, y: 0, z: 0 },
      end: {
        x: target.x + spinX,
        y: target.y + spinY,
        z: target.z + spinZ
      },
      target
    };
  }, [value, order]);

  useFrame(({ clock }) => {
    if (!meshRef.current) return;
    if (startedAt.current == null) startedAt.current = clock.getElapsedTime();

    const elapsed = clock.getElapsedTime() - startedAt.current;
    const t = Math.min(1, elapsed / duration);
    const eased = 1 - Math.pow(1 - t, 3);

    const rx = motion.start.x + (motion.end.x - motion.start.x) * eased;
    const ry = motion.start.y + (motion.end.y - motion.start.y) * eased;
    const rz = motion.start.z + (motion.end.z - motion.start.z) * eased;
    meshRef.current.rotation.set(rx, ry, rz);

    // slight settle drop for a cleaner finish
    const yDrop = t < 1 ? 0.62 - 0.08 * Math.sin(t * Math.PI) : 0.62;
    meshRef.current.parent.position.y = yDrop;

    if (t >= 1) {
      meshRef.current.rotation.set(motion.target.x, motion.target.y, motion.target.z);
      meshRef.current.parent.position.y = 0.62;
    }
  });

  return (
    <group position={position}>
      <group ref={meshRef}>
        <mesh castShadow receiveShadow>
          <boxGeometry args={[1, 1, 1]} />
          <meshStandardMaterial color="#f8fafc" roughness={0.24} metalness={0.08} />
        </mesh>

        <FacePips value={1} rotation={[0, 0, 0]} />
        <FacePips value={2} rotation={[0, Math.PI / 2, 0]} />
        <FacePips value={3} rotation={[-Math.PI / 2, 0, 0]} />
        <FacePips value={4} rotation={[Math.PI / 2, 0, 0]} />
        <FacePips value={5} rotation={[0, -Math.PI / 2, 0]} />
        <FacePips value={6} rotation={[0, Math.PI, 0]} />
      </group>
    </group>
  );
}

function Scene({ diceValues, seed }) {
  const positions = useMemo(() => {
    const gap = 1.55;
    const startX = -((diceValues.length - 1) * gap) / 2;
    return diceValues.map((_, idx) => [startX + idx * gap, 0.62, 0]);
  }, [diceValues]);

  return (
    <>
      <ambientLight intensity={0.52} />
      <directionalLight position={[4, 5, 2]} intensity={1.2} castShadow />
      <pointLight position={[-4, 2, -2]} intensity={0.26} />

      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, -0.06, 0]} receiveShadow>
        <planeGeometry args={[24, 16]} />
        <meshStandardMaterial color="#e2e8f0" />
      </mesh>

      {diceValues.map((value, idx) => (
        <RollingDie
          key={`${seed}-${idx}-${value}`}
          value={value}
          position={positions[idx]}
          order={idx}
        />
      ))}

      <Environment preset="city" />
    </>
  );
}

export default function DiceRoll3DOverlay({ visible, diceValues, onDone, seed = 1 }) {
  if (!visible || !diceValues?.length) return null;

  return (
    <div className="dice-roll-overlay" role="presentation">
      <div className="dice-roll-canvas-wrap">
        <Canvas shadows camera={{ position: [0, 4.2, 6.6], fov: 40 }}>
          <Scene diceValues={diceValues} seed={seed} />
        </Canvas>
      </div>
      <button className="button ghost dice-roll-skip" onClick={onDone}>건너뛰기</button>
    </div>
  );
}
