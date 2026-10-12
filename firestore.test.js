const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');

const PROJECT_ID = 'demo-no-project';
const DB_ID = 'ai-studio-android-vhoqxeth-72b1dc1c-3df7-45db-85d8-6272f418425e';

test('Firestore Security Rules for IP-Only Vhoqxeth', async (t) => {
  let testEnv;

  try {
    testEnv = await initializeTestEnvironment({
      projectId: PROJECT_ID,
      firestore: {
        host: '127.0.0.1',
        port: 8085,
        rules: fs.readFileSync('firestore.rules', 'utf8'),
      },
    });
  } catch (err) {
    console.error('Failed to initialize test environment:', err);
    throw err;
  }

  t.after(async () => {
    if (testEnv) await testEnv.cleanup();
  });

  await t.test('allow unauthenticated read of connection_probes', async () => {
    const unauthedDb = testEnv.unauthenticatedContext().firestore();
    const probeDoc = unauthedDb.collection('connection_probes').doc('test-probe');
    await assertSucceeds(probeDoc.get());
  });

  await t.test('allow valid connection probe write', async () => {
    const unauthedDb = testEnv.unauthenticatedContext().firestore();
    const probeDoc = unauthedDb.collection('connection_probes').doc('health-check');
    await assertSucceeds(
      probeDoc.set({
        status: 'ONLINE',
        databaseId: DB_ID,
        clientApp: 'Vhoqxeth Android',
      })
    );
  });

  await t.test('allow unauthenticated read of registered_ips', async () => {
    const unauthedDb = testEnv.unauthenticatedContext().firestore();
    const ipDoc = unauthedDb.collection('registered_ips').doc('192_168_1_1');
    await assertSucceeds(ipDoc.get());
  });

  await t.test('allow unauthenticated write to registered_ips with valid schema', async () => {
    const unauthedDb = testEnv.unauthenticatedContext().firestore();
    const ipDoc = unauthedDb.collection('registered_ips').doc('192_168_1_1');
    await assertSucceeds(
      ipDoc.set({
        ip: '192.168.1.1',
        userId: 'alice_handle',
        customUserId: 'alice_handle',
        displayName: 'Alice',
        registeredAt: '2026-10-09T00:00:00Z',
      })
    );
  });

  await t.test('reject registered_ips write with missing fields', async () => {
    const unauthedDb = testEnv.unauthenticatedContext().firestore();
    const ipDoc = unauthedDb.collection('registered_ips').doc('bad_ip');
    await assertFails(
      ipDoc.set({
        ip: '192.168.1.1',
        // missing userId, customUserId, displayName
      })
    );
  });

  await t.test('allow unauthenticated read and write of user profile with valid schema', async () => {
    const unauthedDb = testEnv.unauthenticatedContext().firestore();
    const userDoc = unauthedDb.collection('users').doc('user123');
    await assertSucceeds(
      userDoc.set({
        userId: 'user123',
        displayName: 'Alex',
        customUserId: 'alex_99',
      })
    );
    await assertSucceeds(userDoc.get());
  });

  await t.test('reject user profile write missing customUserId', async () => {
    const unauthedDb = testEnv.unauthenticatedContext().firestore();
    const userDoc = unauthedDb.collection('users').doc('bad_user');
    await assertFails(
      userDoc.set({
        userId: 'bad_user',
        displayName: 'No Handle',
      })
    );
  });

  await t.test('reject user profile write with mismatched userId', async () => {
    const unauthedDb = testEnv.unauthenticatedContext().firestore();
    const userDoc = unauthedDb.collection('users').doc('correct_id');
    await assertFails(
      userDoc.set({
        userId: 'wrong_id',
        displayName: 'Mismatched',
        customUserId: 'handle_1',
      })
    );
  });
});
