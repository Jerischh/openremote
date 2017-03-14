package org.openremote.test.user

import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.server.notification.NotificationService
import org.openremote.manager.shared.notification.NotificationResource
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification

import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID
import static org.openremote.model.Constants.MASTER_REALM

class NotificationServiceTest extends Specification implements ManagerContainerTrait {

    def "Store notification tokens for user devices"() {

        given: "the server container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
        def notificationService = container.getService(NotificationService.class)

        and: "an authenticated test user"
        def realm = MASTER_REALM
        def accessToken = authenticate(
                container,
                realm,
                KEYCLOAK_CLIENT_ID,
                "testuser1",
                "testuser1"
        ).token

        and: "the notification resource"
        def client = createClient(container).build()
        def serverUri = serverUri(serverPort)
        def notificationResource = getClientTarget(client, serverUri, realm, accessToken).proxy(NotificationResource.class)

        when: "the notification tokens of some devices are stored"
        notificationResource.storeDeviceToken(null, "device123", "token123")
        notificationResource.storeDeviceToken(null, "device456", "token456")

        then: "the tokens should be in the database"
        notificationService.findDeviceToken("device123", keycloakDemoSetup.testuser1Id) == "token123"
        notificationService.findDeviceToken("device456", keycloakDemoSetup.testuser1Id) == "token456"

        when: "the notification token of some device is updated"
        notificationResource.storeDeviceToken(null, "device456", "token789")

        then: "the updated token should be in the database"
        notificationService.findDeviceToken("device456", keycloakDemoSetup.testuser1Id) == "token789"

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
