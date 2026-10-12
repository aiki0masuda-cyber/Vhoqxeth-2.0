package com.example.model

data class FeatureItem(
    val title: String,
    val description: String,
    val tag: String
)

data class LegalSection(
    val title: String,
    val content: String
)

object LegalContent {
    /**
     * User-facing features shown in 'About the App'.
     * CRITICAL: Admin panels, masterkeys, and developer overrides are strictly omitted.
     */
    val appFeatures = listOf(
        FeatureItem(
            title = "Quantum-Grade Cryptography",
            description = "End-to-end encrypted communication pipelines using post-quantum cryptographic primitives to guarantee that only intended recipients can read transmissions.",
            tag = "SECURITY"
        ),
        FeatureItem(
            title = "IP-Based Zero-Trust Identity",
            description = "Direct device network routing that verifies endpoint network coordinates for instant session recognition without vulnerable passwords or email trackers.",
            tag = "IDENTITY"
        ),
        FeatureItem(
            title = "Decentralized Mesh Channels",
            description = "Encrypted messaging threads routed through high-throughput cloud envelopes with zero server-side telemetry or conversational indexing.",
            tag = "ROUTING"
        ),
        FeatureItem(
            title = "Seamless Multi-Network Roaming",
            description = "Dynamic network discovery that links new Wi-Fi and cellular IP addresses smoothly to your identity as you move between locations.",
            tag = "MOBILITY"
        ),
        FeatureItem(
            title = "Local Data Sovereignty",
            description = "Session states and cryptographic handles are kept exclusively on local device storage. You maintain total ownership of your communications.",
            tag = "PRIVACY"
        )
    )

    val privacyPolicySections = listOf(
        LegalSection(
            title = "1. Zero-Knowledge Architecture",
            content = "Vhoqxeth does not collect personal identifiers, phone numbers, email addresses, contact books, or location telemetry. Your presence on the mesh exists purely through your chosen custom handle and active network endpoint."
        ),
        LegalSection(
            title = "2. Device IP Routing",
            content = "Your device IP is utilized strictly for packet delivery and endpoint recognition in the distributed database. IP addresses are sanitized into route identifiers and can be unlinked at any time via your device profile."
        ),
        LegalSection(
            title = "3. End-to-End Encryption",
            content = "All conversation threads and message payloads are encrypted prior to leaving your device. Cloud coordinators and relays act only as blind transport mechanisms and cannot decrypt your data."
        ),
        LegalSection(
            title = "4. Data Retention & Erasure",
            content = "Signing out or resetting your account clears all local session keys immediately. You retain the right to terminate your network identity and purge linked routing entries at any time."
        )
    )

    val termsAndConditionsSections = listOf(
        LegalSection(
            title = "1. Acceptance of Protocol Rules",
            content = "By establishing a connection to the Vhoqxeth network, you agree to adhere to decentralized communication standards and comply with all applicable local telecommunication laws."
        ),
        LegalSection(
            title = "2. Network Integrity & Non-Abuse",
            content = "Users must not engage in distributed denial-of-service (DDoS) attacks, automated packet flooding, protocol exploitation, or unauthorized impersonation of other network handles."
        ),
        LegalSection(
            title = "3. Client Security Responsibility",
            content = "Security of the communication endpoint depends on physical and software protection of your Android device. Vhoqxeth cannot prevent compromise if the underlying operating system is untrusted or compromised."
        ),
        LegalSection(
            title = "4. Protocol Upgrades & Service Continuity",
            content = "The network protocol evolves to maintain resistance against emerging cryptographic vulnerabilities. Forward-compatible adjustments to database schemas and packet formats may occur automatically."
        )
    )
}
