/**
 * 撤销/重做属性测试
 * Feature: platform-deep-optimization
 * 
 * **Property 8: 撤销/重做一致性**
 * 
 * **Validates: Requirements 2.6, 2.7, 2.8**
 * - 2.6: 撤销操作应恢复到上一个状态
 * - 2.7: 重做操作应恢复到撤销前的状态
 * - 2.8: 撤销/重做历史应正确维护
 * 
 * 验证属性:
 * 1. 撤销操作后状态恢复到上一个状态
 * 2. 重做操作后状态恢复到撤销前的状态
 * 3. 撤销后再重做应得到与原始操作相同的结果
 * 4. 历史记录栈正确维护（最大50步）
 * 5. 新操作会清空重做栈
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import { useHistory } from '../useHistory'

// ============================================================================
// Types
// ============================================================================

/** Test state type for property tests */
interface TestState {
  id: number
  name: string
  value: number
  nested?: {
    data: string[]
  }
}

/** Operation type for simulating user actions */
type Operation = 
  | { type: 'push'; state: TestState; description?: string }
  | { type: 'undo' }
  | { type: 'redo' }
  | { type: 'clear' }

// ============================================================================
// Arbitraries (Test Data Generators)
// ============================================================================


/**
 * Generate a valid test state
 */
const testStateArb: fc.Arbitrary<TestState> = fc.oneof(
  fc.record({
    id: fc.integer({ min: 1, max: 10000 }),
    name: fc.string({ minLength: 1, maxLength: 50 }),
    value: fc.integer({ min: -1000, max: 1000 }),
    nested: fc.record({
      data: fc.array(fc.string({ minLength: 1, maxLength: 20 }), { minLength: 0, maxLength: 5 })
    })
  }),
  fc.record({
    id: fc.integer({ min: 1, max: 10000 }),
    name: fc.string({ minLength: 1, maxLength: 50 }),
    value: fc.integer({ min: -1000, max: 1000 }),
  })
)

/**
 * Generate a push operation
 */
const pushOperationArb: fc.Arbitrary<Operation> = fc.oneof(
  fc.record({
    type: fc.constant('push' as const),
    state: testStateArb,
    description: fc.string({ minLength: 1, maxLength: 100 })
  }),
  fc.record({
    type: fc.constant('push' as const),
    state: testStateArb,
  })
)

/**
 * Generate an undo operation
 */
const undoOperationArb: fc.Arbitrary<Operation> = fc.constant({ type: 'undo' as const })

/**
 * Generate a redo operation
 */
const redoOperationArb: fc.Arbitrary<Operation> = fc.constant({ type: 'redo' as const })

/**
 * Generate a clear operation
 */
const clearOperationArb: fc.Arbitrary<Operation> = fc.constant({ type: 'clear' as const })

/**
 * Generate any operation
 */
const operationArb: fc.Arbitrary<Operation> = fc.oneof(
  { weight: 5, arbitrary: pushOperationArb },
  { weight: 2, arbitrary: undoOperationArb },
  { weight: 2, arbitrary: redoOperationArb },
  { weight: 1, arbitrary: clearOperationArb }
)

/**
 * Generate a sequence of operations
 */
const operationSequenceArb = (minLength: number, maxLength: number): fc.Arbitrary<Operation[]> =>
  fc.array(operationArb, { minLength, maxLength })

/**
 * Generate a sequence of push operations only
 */
const pushSequenceArb = (minLength: number, maxLength: number): fc.Arbitrary<Operation[]> =>
  fc.array(pushOperationArb, { minLength, maxLength })

/**
 * Generate a sequence of states (for simpler tests)
 */
const stateSequenceArb = (minLength: number, maxLength: number): fc.Arbitrary<TestState[]> =>
  fc.array(testStateArb, { minLength, maxLength })


// ============================================================================
// Helper Functions
// ============================================================================

/**
 * Deep equality check for test states
 */
function statesEqual(a: TestState | null | undefined, b: TestState | null | undefined): boolean {
  if (a == null && b == null) return true
  if (a == null || b == null) return false
  return JSON.stringify(a) === JSON.stringify(b)
}

/**
 * Execute a sequence of operations on a history instance
 */
function executeOperations<T>(
  history: ReturnType<typeof useHistory<T>>,
  operations: Operation[]
): void {
  for (const op of operations) {
    switch (op.type) {
      case 'push':
        history.push(op.state as T, op.description)
        break
      case 'undo':
        history.undo()
        break
      case 'redo':
        history.redo()
        break
      case 'clear':
        history.clear()
        break
    }
  }
}

/**
 * Get the expected undo stack size after operations
 * (simplified model for verification)
 */
function simulateHistory<T>(operations: Operation[], maxSize: number = 50): {
  undoStack: T[]
  redoStack: T[]
  currentState: T | null
} {
  const undoStack: T[] = []
  const redoStack: T[] = []
  let currentState: T | null = null

  for (const op of operations) {
    switch (op.type) {
      case 'push':
        if (currentState !== null) {
          undoStack.push(currentState)
          if (undoStack.length > maxSize) {
            undoStack.shift()
          }
        }
        currentState = op.state as T
        redoStack.length = 0 // Clear redo stack
        break
      case 'undo':
        if (undoStack.length > 0) {
          if (currentState !== null) {
            redoStack.push(currentState)
          }
          currentState = undoStack.pop()!
        }
        break
      case 'redo':
        if (redoStack.length > 0) {
          if (currentState !== null) {
            undoStack.push(currentState)
          }
          currentState = redoStack.pop()!
        }
        break
      case 'clear':
        undoStack.length = 0
        redoStack.length = 0
        currentState = null
        break
    }
  }

  return { undoStack, redoStack, currentState }
}


// ============================================================================
// Property-Based Tests
// ============================================================================

describe('Undo/Redo Property Tests', () => {
  /**
   * Property 8: 撤销/重做一致性
   * 
   * 验证撤销/重做操作在任意操作序列下都能保持状态一致性
   * 
   * **Validates: Requirements 2.6, 2.7, 2.8**
   */
  describe('Property 8: Undo/Redo Consistency', () => {
    // ========================================================================
    // 8.1 Undo Restores Previous State
    // ========================================================================
    describe('8.1 Undo operation restores to the previous state', () => {
      it('should restore to previous state after single undo', () => {
        fc.assert(
          fc.property(
            fc.tuple(testStateArb, testStateArb),
            ([state1, state2]) => {
              const history = useHistory<TestState>()
              
              history.push(state1, 'first')
              history.push(state2, 'second')
              
              // After undo, should return to state1
              const undoneState = history.undo()
              
              return statesEqual(undoneState, state1) &&
                     statesEqual(history.currentState.value, state1)
            }
          ),
          { numRuns: 200 }
        )
      })

      it('should restore states in reverse order with multiple undos', () => {
        fc.assert(
          fc.property(
            stateSequenceArb(3, 10),
            (states) => {
              const history = useHistory<TestState>()
              
              // Push all states
              for (const state of states) {
                history.push(state)
              }
              
              // Undo all and verify reverse order
              for (let i = states.length - 2; i >= 0; i--) {
                const undoneState = history.undo()
                if (!statesEqual(undoneState, states[i])) {
                  return false
                }
              }
              
              return true
            }
          ),
          { numRuns: 200 }
        )
      })

      it('should return null when undo stack is empty', () => {
        fc.assert(
          fc.property(
            testStateArb,
            (state) => {
              const history = useHistory<TestState>()
              
              // Push one state
              history.push(state)
              
              // First undo should return null (no previous state)
              const result = history.undo()
              
              return result === null && history.canUndo.value === false
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should correctly update canUndo flag', () => {
        fc.assert(
          fc.property(
            stateSequenceArb(2, 5),
            (states) => {
              const history = useHistory<TestState>()
              
              // Initially cannot undo
              if (history.canUndo.value) return false
              
              // Push states
              for (let i = 0; i < states.length; i++) {
                history.push(states[i]!)
                // After first push, still cannot undo (no previous state)
                // After second push, can undo
                if (i >= 1 && !history.canUndo.value) return false
              }
              
              // Undo until empty
              while (history.canUndo.value) {
                history.undo()
              }
              
              // Should not be able to undo anymore
              return !history.canUndo.value
            }
          ),
          { numRuns: 100 }
        )
      })
    })


    // ========================================================================
    // 8.2 Redo Restores State Before Undo
    // ========================================================================
    describe('8.2 Redo operation restores to the state before undo', () => {
      it('should restore to state before undo after single redo', () => {
        fc.assert(
          fc.property(
            fc.tuple(testStateArb, testStateArb),
            ([state1, state2]) => {
              const history = useHistory<TestState>()
              
              history.push(state1, 'first')
              history.push(state2, 'second')
              
              // Undo then redo
              history.undo()
              const redoneState = history.redo()
              
              return statesEqual(redoneState, state2) &&
                     statesEqual(history.currentState.value, state2)
            }
          ),
          { numRuns: 200 }
        )
      })

      it('should restore states in correct order with multiple redos', () => {
        fc.assert(
          fc.property(
            stateSequenceArb(3, 10),
            (states) => {
              const history = useHistory<TestState>()
              
              // Push all states
              for (const state of states) {
                history.push(state)
              }
              
              // Undo all
              const undoCount = states.length - 1
              for (let i = 0; i < undoCount; i++) {
                history.undo()
              }
              
              // Redo all and verify order
              for (let i = 1; i < states.length; i++) {
                const redoneState = history.redo()
                if (!statesEqual(redoneState, states[i])) {
                  return false
                }
              }
              
              return true
            }
          ),
          { numRuns: 200 }
        )
      })

      it('should return null when redo stack is empty', () => {
        fc.assert(
          fc.property(
            testStateArb,
            (state) => {
              const history = useHistory<TestState>()
              
              history.push(state)
              
              // No undo performed, redo should return null
              const result = history.redo()
              
              return result === null && history.canRedo.value === false
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should correctly update canRedo flag', () => {
        fc.assert(
          fc.property(
            stateSequenceArb(3, 5),
            (states) => {
              const history = useHistory<TestState>()
              
              // Initially cannot redo
              if (history.canRedo.value) return false
              
              // Push states
              for (const state of states) {
                history.push(state)
                // After push, cannot redo
                if (history.canRedo.value) return false
              }
              
              // Undo once
              history.undo()
              
              // Now can redo
              if (!history.canRedo.value) return false
              
              // Redo
              history.redo()
              
              // Cannot redo anymore
              return !history.canRedo.value
            }
          ),
          { numRuns: 100 }
        )
      })
    })


    // ========================================================================
    // 8.3 Undo followed by Redo returns to same state
    // ========================================================================
    describe('8.3 Undo followed by redo returns to the same state', () => {
      it('should return to exact same state after undo then redo', () => {
        fc.assert(
          fc.property(
            stateSequenceArb(2, 10),
            (states) => {
              const history = useHistory<TestState>()
              
              // Push all states
              for (const state of states) {
                history.push(state)
              }
              
              const stateBeforeUndo = history.currentState.value
              
              // Undo then redo
              history.undo()
              history.redo()
              
              return statesEqual(history.currentState.value, stateBeforeUndo)
            }
          ),
          { numRuns: 300 }
        )
      })

      it('should return to same state after multiple undo/redo cycles', () => {
        fc.assert(
          fc.property(
            fc.tuple(
              stateSequenceArb(3, 8),
              fc.integer({ min: 1, max: 5 })
            ),
            ([states, cycles]) => {
              const history = useHistory<TestState>()
              
              // Push all states
              for (const state of states) {
                history.push(state)
              }
              
              const originalState = history.currentState.value
              
              // Perform multiple undo/redo cycles
              for (let i = 0; i < cycles; i++) {
                history.undo()
                history.redo()
              }
              
              return statesEqual(history.currentState.value, originalState)
            }
          ),
          { numRuns: 200 }
        )
      })

      it('should maintain state consistency through arbitrary undo/redo sequence', () => {
        fc.assert(
          fc.property(
            fc.tuple(
              stateSequenceArb(5, 15),
              fc.integer({ min: 1, max: 10 })
            ),
            ([states, undoCount]) => {
              const history = useHistory<TestState>()
              
              // Push all states
              for (const state of states) {
                history.push(state)
              }
              
              // Perform limited undos
              const actualUndos = Math.min(undoCount, states.length - 1)
              for (let i = 0; i < actualUndos; i++) {
                history.undo()
              }
              
              // Redo all
              for (let i = 0; i < actualUndos; i++) {
                history.redo()
              }
              
              // Should be back to the last pushed state
              return statesEqual(history.currentState.value, states[states.length - 1])
            }
          ),
          { numRuns: 200 }
        )
      })

      it('should preserve state integrity through complex undo/redo patterns', () => {
        fc.assert(
          fc.property(
            stateSequenceArb(5, 10),
            (states) => {
              const history = useHistory<TestState>()
              
              // Push all states
              for (const state of states) {
                history.push(state)
              }
              
              // Complex pattern: undo 3, redo 2, undo 1, redo all
              const undos1 = Math.min(3, states.length - 1)
              for (let i = 0; i < undos1; i++) history.undo()
              
              const redos1 = Math.min(2, undos1)
              for (let i = 0; i < redos1; i++) history.redo()
              
              const undos2 = 1
              for (let i = 0; i < undos2; i++) history.undo()
              
              // Redo all remaining
              while (history.canRedo.value) {
                history.redo()
              }
              
              // Should be back to last state
              return statesEqual(history.currentState.value, states[states.length - 1])
            }
          ),
          { numRuns: 200 }
        )
      })
    })


    // ========================================================================
    // 8.4 History Stack Correctly Maintained (Max 50 Steps)
    // ========================================================================
    describe('8.4 History stack is correctly maintained with max 50 steps', () => {
      it('should limit undo stack to maxSize', () => {
        fc.assert(
          fc.property(
            fc.integer({ min: 10, max: 30 }),
            (maxSize) => {
              const history = useHistory<TestState>({ maxSize })
              
              // Push more states than maxSize
              for (let i = 0; i < maxSize + 10; i++) {
                history.push({ id: i, name: `state${i}`, value: i })
              }
              
              // Undo stack should be limited to maxSize
              return history.undoStackSize.value <= maxSize
            }
          ),
          { numRuns: 50 }
        )
      })

      it('should preserve most recent states when exceeding maxSize', () => {
        const maxSize = 5
        const history = useHistory<TestState>({ maxSize })
        
        // Push 10 states (more than maxSize)
        for (let i = 0; i < 10; i++) {
          history.push({ id: i, name: `state${i}`, value: i })
        }
        
        // Should be able to undo maxSize times
        let undoCount = 0
        while (history.canUndo.value) {
          history.undo()
          undoCount++
        }
        
        // Should have undone exactly maxSize times
        expect(undoCount).toBe(maxSize)
        
        // Current state should be state 4 (10 - 5 - 1 = 4)
        expect(history.currentState.value?.id).toBe(4)
      })

      it('should correctly track undoStackSize', () => {
        fc.assert(
          fc.property(
            stateSequenceArb(1, 20),
            (states) => {
              const history = useHistory<TestState>({ maxSize: 50 })
              
              for (let i = 0; i < states.length; i++) {
                history.push(states[i]!)
                // After i+1 pushes, undo stack should have i entries (current state not in stack)
                const expectedSize = Math.min(i, 50)
                if (history.undoStackSize.value !== expectedSize) {
                  return false
                }
              }
              
              return true
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should correctly track redoStackSize', () => {
        fc.assert(
          fc.property(
            stateSequenceArb(3, 10),
            (states) => {
              const history = useHistory<TestState>()
              
              // Push all states
              for (const state of states) {
                history.push(state)
              }
              
              // Undo some and check redo stack size
              const undoCount = Math.min(states.length - 1, 5)
              for (let i = 0; i < undoCount; i++) {
                history.undo()
                if (history.redoStackSize.value !== i + 1) {
                  return false
                }
              }
              
              return true
            }
          ),
          { numRuns: 100 }
        )
      })
    })


    // ========================================================================
    // 8.5 New Operation Clears Redo Stack
    // ========================================================================
    describe('8.5 New push operation clears the redo stack', () => {
      it('should clear redo stack when pushing new state after undo', () => {
        fc.assert(
          fc.property(
            fc.tuple(testStateArb, testStateArb, testStateArb),
            ([state1, state2, state3]) => {
              const history = useHistory<TestState>()
              
              history.push(state1)
              history.push(state2)
              
              // Undo to create redo stack
              history.undo()
              
              // Verify redo is available
              if (!history.canRedo.value) return false
              
              // Push new state
              history.push(state3)
              
              // Redo stack should be cleared
              return !history.canRedo.value && history.redoStackSize.value === 0
            }
          ),
          { numRuns: 200 }
        )
      })

      it('should not be able to redo after pushing new state', () => {
        fc.assert(
          fc.property(
            stateSequenceArb(3, 8),
            (states) => {
              const history = useHistory<TestState>()
              
              // Push initial states
              for (const state of states) {
                history.push(state)
              }
              
              // Undo multiple times
              const undoCount = Math.min(3, states.length - 1)
              for (let i = 0; i < undoCount; i++) {
                history.undo()
              }
              
              // Push a new state
              history.push({ id: 9999, name: 'new', value: 9999 })
              
              // Should not be able to redo
              const redoResult = history.redo()
              
              return redoResult === null && !history.canRedo.value
            }
          ),
          { numRuns: 200 }
        )
      })

      it('should maintain undo history after clearing redo stack', () => {
        fc.assert(
          fc.property(
            stateSequenceArb(4, 10),
            (states) => {
              const history = useHistory<TestState>()
              
              // Push initial states
              for (const state of states) {
                history.push(state)
              }
              
              // Undo twice
              history.undo()
              history.undo()
              
              const stateBeforeNewPush = history.currentState.value
              
              // Push new state (clears redo)
              const newState = { id: 9999, name: 'new', value: 9999 }
              history.push(newState)
              
              // Undo should return to state before new push
              const undoneState = history.undo()
              
              return statesEqual(undoneState, stateBeforeNewPush)
            }
          ),
          { numRuns: 200 }
        )
      })
    })


    // ========================================================================
    // 8.6 State Isolation (Deep Clone)
    // ========================================================================
    describe('8.6 States are properly isolated (deep cloned)', () => {
      it('should not be affected by mutations to original state object', () => {
        // Use explicit test case instead of property test to avoid edge cases
        // with fast-check generated objects that have null prototype
        const history = useHistory<TestState>({ deepClone: true })
        
        const state: TestState = { id: 1, name: 'original', value: 100 }
        const originalId = state.id
        const originalName = state.name
        const originalValue = state.value
        
        history.push(state)
        
        // Mutate the original state object AFTER push
        state.id = 99999
        state.name = 'mutated'
        state.value = -99999
        
        // History should have the original values (not the mutated ones)
        expect(history.currentState.value?.id).toBe(originalId)
        expect(history.currentState.value?.name).toBe(originalName)
        expect(history.currentState.value?.value).toBe(originalValue)
      })

      it('should not be affected by mutations to returned state', () => {
        const history = useHistory<TestState>({ deepClone: true })
        
        const state1: TestState = { id: 1, name: 'first', value: 100 }
        const state2: TestState = { id: 2, name: 'second', value: 200 }
        
        const originalId = state1.id
        const originalName = state1.name
        
        history.push(state1)
        history.push(state2)
        
        // Get state via undo
        const undoneState = history.undo()
        expect(undoneState).not.toBeNull()
        
        // Mutate the returned state
        undoneState!.id = 99999
        undoneState!.name = 'mutated'
        
        // Redo and undo again - should get original values (not mutated)
        history.redo()
        const undoneAgain = history.undo()
        
        expect(undoneAgain?.id).toBe(originalId)
        expect(undoneAgain?.name).toBe(originalName)
      })

      it('should properly clone nested objects', () => {
        const history = useHistory<TestState>({ deepClone: true })
        
        const state = {
          id: 1,
          name: 'test',
          value: 100,
          nested: { data: ['a', 'b', 'c'] }
        }
        
        history.push(state)
        
        // Mutate nested data
        state.nested.data.push('mutated')
        state.nested.data[0] = 'changed'
        
        // History should have original nested data
        expect(history.currentState.value?.nested?.data).toEqual(['a', 'b', 'c'])
      })
    })


    // ========================================================================
    // 8.7 Clear Operation
    // ========================================================================
    describe('8.7 Clear operation resets all history', () => {
      it('should clear all stacks and current state', () => {
        fc.assert(
          fc.property(
            stateSequenceArb(3, 10),
            (states) => {
              const history = useHistory<TestState>()
              
              // Push states
              for (const state of states) {
                history.push(state)
              }
              
              // Undo some to create redo stack
              history.undo()
              history.undo()
              
              // Clear
              history.clear()
              
              return history.undoStackSize.value === 0 &&
                     history.redoStackSize.value === 0 &&
                     history.currentState.value === null &&
                     !history.canUndo.value &&
                     !history.canRedo.value
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should allow new operations after clear', () => {
        fc.assert(
          fc.property(
            fc.tuple(stateSequenceArb(2, 5), testStateArb),
            ([states, newState]) => {
              const history = useHistory<TestState>()
              
              // Push initial states
              for (const state of states) {
                history.push(state)
              }
              
              // Clear
              history.clear()
              
              // Push new state
              history.push(newState)
              
              return statesEqual(history.currentState.value, newState) &&
                     history.undoStackSize.value === 0 // First push has no undo
            }
          ),
          { numRuns: 100 }
        )
      })
    })


    // ========================================================================
    // 8.8 Arbitrary Operation Sequences
    // ========================================================================
    describe('8.8 Arbitrary operation sequences maintain consistency', () => {
      it('should maintain valid state through random operation sequences', () => {
        fc.assert(
          fc.property(
            operationSequenceArb(5, 30),
            (operations) => {
              const history = useHistory<TestState>({ maxSize: 50 })
              
              // Execute all operations
              executeOperations(history, operations)
              
              // Verify invariants
              // 1. Stack sizes should be non-negative
              if (history.undoStackSize.value < 0) return false
              if (history.redoStackSize.value < 0) return false
              
              // 2. canUndo should match undoStackSize > 0
              if (history.canUndo.value !== (history.undoStackSize.value > 0)) return false
              
              // 3. canRedo should match redoStackSize > 0
              if (history.canRedo.value !== (history.redoStackSize.value > 0)) return false
              
              // 4. Undo stack should not exceed maxSize
              if (history.undoStackSize.value > 50) return false
              
              return true
            }
          ),
          { numRuns: 500 }
        )
      })

      it('should match simulated history state', () => {
        fc.assert(
          fc.property(
            pushSequenceArb(3, 15),
            (operations) => {
              const history = useHistory<TestState>({ maxSize: 50 })
              
              // Execute operations
              executeOperations(history, operations)
              
              // Simulate the same operations
              const simulated = simulateHistory<TestState>(operations, 50)
              
              // Compare stack sizes
              if (history.undoStackSize.value !== simulated.undoStack.length) return false
              if (history.redoStackSize.value !== simulated.redoStack.length) return false
              
              // Compare current state
              return statesEqual(
                history.currentState.value,
                simulated.currentState
              )
            }
          ),
          { numRuns: 300 }
        )
      })

      it('should handle rapid undo/redo sequences', () => {
        fc.assert(
          fc.property(
            fc.tuple(
              stateSequenceArb(5, 10),
              fc.array(fc.boolean(), { minLength: 10, maxLength: 30 })
            ),
            ([states, undoRedoPattern]) => {
              const history = useHistory<TestState>()
              
              // Push all states
              for (const state of states) {
                history.push(state)
              }
              
              // Execute rapid undo/redo based on pattern
              for (const doUndo of undoRedoPattern) {
                if (doUndo && history.canUndo.value) {
                  history.undo()
                } else if (!doUndo && history.canRedo.value) {
                  history.redo()
                }
              }
              
              // Verify invariants still hold
              return history.undoStackSize.value >= 0 &&
                     history.redoStackSize.value >= 0 &&
                     history.canUndo.value === (history.undoStackSize.value > 0) &&
                     history.canRedo.value === (history.redoStackSize.value > 0)
            }
          ),
          { numRuns: 200 }
        )
      })
    })


    // ========================================================================
    // 8.9 Edge Cases
    // ========================================================================
    describe('8.9 Edge cases are handled correctly', () => {
      it('should handle empty history gracefully', () => {
        const history = useHistory<TestState>()
        
        expect(history.canUndo.value).toBe(false)
        expect(history.canRedo.value).toBe(false)
        expect(history.undoStackSize.value).toBe(0)
        expect(history.redoStackSize.value).toBe(0)
        expect(history.currentState.value).toBe(null)
        expect(history.undo()).toBe(null)
        expect(history.redo()).toBe(null)
      })

      it('should handle single state correctly', () => {
        fc.assert(
          fc.property(
            testStateArb,
            (state) => {
              const history = useHistory<TestState>()
              
              history.push(state)
              
              // Should have current state but no undo history
              return statesEqual(history.currentState.value, state) &&
                     history.undoStackSize.value === 0 &&
                     !history.canUndo.value
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should handle maxSize of 1', () => {
        const history = useHistory<TestState>({ maxSize: 1 })
        
        history.push({ id: 1, name: 'first', value: 1 })
        history.push({ id: 2, name: 'second', value: 2 })
        history.push({ id: 3, name: 'third', value: 3 })
        
        // Should only be able to undo once
        expect(history.undoStackSize.value).toBe(1)
        
        const undone = history.undo()
        expect(undone?.id).toBe(2)
        
        // No more undo available
        expect(history.canUndo.value).toBe(false)
      })

      it('should handle null/undefined in nested objects', () => {
        const history = useHistory<TestState>()
        
        history.push({ id: 1, name: 'test', value: 1 })
        history.push({ id: 2, name: 'test2', value: 2, nested: { data: [] } })
        
        const undone = history.undo()
        expect(undone?.nested).toBeUndefined()
      })

      it('should handle consecutive identical states', () => {
        fc.assert(
          fc.property(
            testStateArb,
            (state) => {
              const history = useHistory<TestState>()
              
              // Push same state multiple times
              history.push(state)
              history.push(state)
              history.push(state)
              
              // Should have 2 entries in undo stack
              return history.undoStackSize.value === 2
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    // ========================================================================
    // 8.10 getUndoStack and getRedoStack
    // ========================================================================
    describe('8.10 Stack accessor methods return correct data', () => {
      it('should return copy of undo stack', () => {
        fc.assert(
          fc.property(
            stateSequenceArb(3, 8),
            (states) => {
              const history = useHistory<TestState>()
              
              for (const state of states) {
                history.push(state)
              }
              
              const stack = history.getUndoStack()
              
              // Should have correct length
              if (stack.length !== states.length - 1) return false
              
              // Modifying returned stack should not affect history
              stack.pop()
              
              return history.undoStackSize.value === states.length - 1
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should return copy of redo stack', () => {
        fc.assert(
          fc.property(
            stateSequenceArb(4, 8),
            (states) => {
              const history = useHistory<TestState>()
              
              for (const state of states) {
                history.push(state)
              }
              
              // Undo twice
              history.undo()
              history.undo()
              
              const stack = history.getRedoStack()
              
              // Should have 2 entries
              if (stack.length !== 2) return false
              
              // Modifying returned stack should not affect history
              stack.pop()
              
              return history.redoStackSize.value === 2
            }
          ),
          { numRuns: 100 }
        )
      })
    })
  })
})
