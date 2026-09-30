package de.werklog.app

/** Saved only inside the encrypted workspace draft, including the origin of cross-links. */
data class WorkspaceRoute(val page: Int, val tool: String? = null, val assetId: String? = null,
    val recordId: String? = null, val entryId: String? = null, val scrollY: Int = 0) : java.io.Serializable {
    fun parent(): WorkspaceRoute = when {
        entryId != null -> copy(entryId = null)
        page == 1 && assetId != null -> WorkspaceRoute(1)
        page == 4 && tool != null && recordId != null -> WorkspaceRoute(4, tool)
        page == 4 && tool != null -> WorkspaceRoute(4)
        page in 1..3 -> WorkspaceRoute(4)
        else -> WorkspaceRoute(0)
    }
}
