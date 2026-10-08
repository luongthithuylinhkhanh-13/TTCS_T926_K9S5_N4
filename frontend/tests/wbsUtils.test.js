import assert from 'node:assert/strict';
import test from 'node:test';
import { recalculateTreeProgress } from '../src/utils/wbsUtils.js';

test('returns an empty tree for missing or empty input', () => {
  assert.deepEqual(recalculateTreeProgress(), []);
  assert.deepEqual(recalculateTreeProgress([]), []);
});

test('recalculates nested phase and project progress from all descendant tasks', () => {
  const tree = [
    {
      id: 'project',
      type: 'project',
      progress: 0,
      status: 'not_started',
      children: [
        {
          id: 'phase-a',
          type: 'phase',
          progress: 0,
          status: 'not_started',
          children: [
            { id: 'task-a1', type: 'task', progress: 100, status: 'completed' },
            { id: 'task-a2', type: 'task', progress: 0, status: 'not_started' },
          ],
        },
        {
          id: 'phase-b',
          type: 'phase',
          progress: 0,
          status: 'not_started',
          children: [{ id: 'task-b1', type: 'task', progress: 0, status: 'not_started' }],
        },
      ],
    },
  ];

  const result = recalculateTreeProgress(tree);

  assert.equal(result[0].progress, 33);
  assert.equal(result[0].status, 'in_progress');
  assert.equal(result[0].children[0].progress, 50);
  assert.equal(result[0].children[0].status, 'in_progress');
  assert.equal(result[0].children[1].progress, 0);
  assert.equal(result[0].children[1].status, 'not_started');
});

test('rolls up complex task networks once per task, independent of dependency links', () => {
  const tree = [
    {
      id: 'project',
      type: 'project',
      progress: 0,
      status: 'not_started',
      children: [
        {
          id: 'phase-foundation',
          type: 'phase',
          progress: 0,
          status: 'not_started',
          children: [
            {
              id: 'task-survey',
              type: 'task',
              progress: 100,
              status: 'completed',
              dependencies: [],
            },
            {
              id: 'task-excavation',
              type: 'task',
              progress: 50,
              status: 'in_progress',
              dependencies: [{ taskId: 'task-survey', type: 'FS' }],
            },
          ],
        },
        {
          id: 'phase-structure',
          type: 'phase',
          progress: 0,
          status: 'not_started',
          children: [
            {
              id: 'task-columns',
              type: 'task',
              progress: 0,
              status: 'not_started',
              dependencies: [{ taskId: 'task-excavation', type: 'SS' }],
            },
            {
              id: 'task-beams',
              type: 'task',
              progress: 75,
              status: 'in_progress',
              dependencies: [
                { taskId: 'task-columns', type: 'FF' },
                { taskId: 'task-excavation', type: 'FS' },
              ],
            },
            {
              id: 'task-roof',
              type: 'task',
              progress: 25,
              status: 'in_progress',
              dependencies: [{ taskId: 'task-beams', type: 'FS' }],
            },
            {
              id: 'task-finishes',
              type: 'task',
              progress: 100,
              status: 'completed',
              dependencies: [{ taskId: 'task-roof', type: 'SS' }],
            },
          ],
        },
      ],
    },
  ];

  const result = recalculateTreeProgress(tree);

  assert.equal(result[0].progress, 58);
  assert.equal(result[0].status, 'in_progress');
  assert.equal(result[0].children[0].progress, 75);
  assert.equal(result[0].children[1].progress, 50);
  assert.equal(result[0].children[1].children[3].progress, 100);
});

test('does not mutate input nodes while recalculating', () => {
  const tree = [
    {
      id: 'project',
      type: 'project',
      progress: 0,
      status: 'not_started',
      children: [{ id: 'task', type: 'task', progress: 40, status: 'in_progress' }],
    },
  ];
  const original = structuredClone(tree);

  recalculateTreeProgress(tree);

  assert.deepEqual(tree, original);
});