/// The type of a block event.
enum BlockEventType {
  /// An app was blocked (block screen shown or shield applied).
  blocked,

  /// An app was unblocked (block screen dismissed or shield removed).
  unblocked,

  /// A blocked app was attempted to be opened.
  attemptedAccess,

  /// A schedule was activated.
  scheduleActivated,

  /// A schedule was deactivated.
  scheduleDeactivated,

  /// A profile was activated.
  profileActivated,

  /// A profile was deactivated.
  profileDeactivated,

  // --- Debug / lifecycle events ---

  /// Android accessibility service connected (started running).
  serviceConnected,

  /// Android accessibility service disconnected (was killed or stopped).
  serviceDisconnected,

  /// Android accessibility service was interrupted by the system.
  serviceInterrupted,

  /// Device boot completed — alarms have been rescheduled.
  bootCompleted,

  /// Blocking was activated (startBlocking called).
  blockingStarted,

  /// Blocking was deactivated (stopBlocking called).
  blockingStopped,

  /// Flutter plugin attached to the engine (app started / engine created).
  pluginAttached,

  /// Alarm fired for schedule start.
  alarmFiredStart,

  /// Alarm fired for schedule end.
  alarmFiredEnd,

  /// Debug log was fetched from the Android buffer (snapshot of current state).
  debugLogFetched,

  /// Unknown event type received from native. Raw type is in [rawType].
  unknown,
}

/// Represents an event that occurred during app blocking.
class BlockEvent {
  /// Creates a [BlockEvent] instance.
  const BlockEvent({
    required this.type,
    required this.timestamp,
    this.packageName,
    this.scheduleId,
    this.profileId,
    this.rawType,
    this.extra,
  });

  /// The type of event.
  final BlockEventType type;

  /// When the event occurred.
  final DateTime timestamp;

  /// The package name of the affected app, if applicable.
  final String? packageName;

  /// The schedule ID that triggered this event, if applicable.
  final String? scheduleId;

  /// The profile ID that triggered this event, if applicable.
  final String? profileId;

  /// The raw type string from native when [type] == [BlockEventType.unknown].
  final String? rawType;

  /// Additional key/value data from the native side (service state, counts, etc.).
  final Map<String, dynamic>? extra;

  /// Creates a [BlockEvent] from a map. Unknown event types are mapped to
  /// [BlockEventType.unknown] instead of throwing.
  factory BlockEvent.fromMap(Map<String, dynamic> map) {
    final typeStr = map['type'] as String? ?? 'unknown';
    BlockEventType type;
    try {
      type = BlockEventType.values.byName(typeStr);
    } catch (_) {
      type = BlockEventType.unknown;
    }
    // Collect all extra fields not in the standard set
    const standardKeys = {'type', 'timestamp', 'packageName', 'scheduleId', 'profileId'};
    final extra = <String, dynamic>{
      for (final e in map.entries)
        if (!standardKeys.contains(e.key)) e.key: e.value,
    };
    return BlockEvent(
      type: type,
      timestamp: DateTime.fromMillisecondsSinceEpoch(
        (map['timestamp'] as num?)?.toInt() ?? DateTime.now().millisecondsSinceEpoch,
      ),
      packageName: map['packageName'] as String?,
      scheduleId: map['scheduleId'] as String?,
      profileId: map['profileId'] as String?,
      rawType: type == BlockEventType.unknown ? typeStr : null,
      extra: extra.isNotEmpty ? extra : null,
    );
  }

  @override
  String toString() =>
      'BlockEvent(type: $type, packageName: $packageName, profileId: $profileId, timestamp: $timestamp)';
}
