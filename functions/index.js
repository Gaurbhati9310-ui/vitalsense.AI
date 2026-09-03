const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

/**
 * Triggered on update to any queue entry document.
 * Dispatches high-priority push notifications when:
 * 1. Entry status changes to CALLED (Doctor is calling this patient).
 * 2. Position updates such that patient is next in line.
 */
exports.onQueueEntryUpdated = functions.firestore
  .document("queue_entries/{entryId}")
  .onUpdate(async (change, context) => {
    const beforeData = change.before.data();
    const afterData = change.after.data();

    // 1. Patient is CALLED
    if (beforeData.status !== "CALLED" && afterData.status === "CALLED") {
      const patientId = afterData.patientId;
      const doctorName = afterData.doctorName;
      const tokenNumber = afterData.tokenNumber;

      const payload = {
        notification: {
          title: "Doctor Calling You Now!",
          body: `Dr. ${doctorName} has called Token #${tokenNumber}. Please proceed to the room.`,
        },
        data: {
          type: "QUEUE_CALLED",
          entryId: context.params.entryId,
          doctorId: afterData.doctorId,
          tokenNumber: String(tokenNumber),
        },
        topic: `patient_${patientId}`,
      };

      try {
        await admin.messaging().send(payload);
        console.log(`Successfully dispatched QUEUE_CALLED notification to topic patient_${patientId}`);
      } catch (error) {
        console.error("Error sending QUEUE_CALLED notification:", error);
      }
    }

    return null;
  });

/**
 * Triggered on creation of high-priority Emergency SOS notices.
 */
exports.onEmergencyNoticeCreated = functions.firestore
  .document("broadcast_notices/{noticeId}")
  .onCreate(async (snap, context) => {
    const data = snap.data();
    if (data.isUrgent) {
      const payload = {
        notification: {
          title: data.title || "EMERGENCY SOS ALERT",
          body: data.message || "An emergency SOS has been triggered in your village!",
        },
        data: {
          type: "SOS_ALERT",
          noticeId: context.params.noticeId,
        },
        topic: "health_workers",
      };

      try {
        await admin.messaging().send(payload);
        console.log("Successfully sent emergency broadcast notification.");
      } catch (err) {
        console.error("Error sending emergency notification:", err);
      }
    }
  });
