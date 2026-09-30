package de.werklog.app

import org.junit.Assert.*
import org.junit.Test

class WorkspaceRouteTest {
    @Test fun backMovesFromDetailsToListToOperationThenToday() {
        val asset = WorkspaceRoute(1, assetId = "asset", scrollY = 320)
        assertEquals(WorkspaceRoute(1), asset.parent())
        assertEquals(WorkspaceRoute(4), asset.parent().parent())
        assertEquals(WorkspaceRoute(0), asset.parent().parent().parent())
        val guide = WorkspaceRoute(4, "Anleitungen", recordId = "guide")
        assertEquals(WorkspaceRoute(4, "Anleitungen"), guide.parent())
        assertEquals(WorkspaceRoute(4), guide.parent().parent())
    }
    @Test fun closingAnEntryKeepsItsUnderlyingRecordAndScrollPosition() {
        val order = WorkspaceRoute(4, "Bestellungen", recordId = "order", entryId = "entry", scrollY = 450)
        assertEquals(order.copy(entryId = null), order.parent())
        assertEquals(WorkspaceRoute(0), WorkspaceRoute(5).parent())
    }
}
