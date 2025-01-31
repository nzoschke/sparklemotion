package baaahs.mapper

import three.js.PerspectiveCamera
import three_ext.CameraControls
import three_ext.Matrix4
import three_ext.toVector3F

fun CameraPosition.Companion.from(
    camera: PerspectiveCamera,
    controls: CameraControls
): CameraPosition =
    CameraPosition(
        controls.getPosition().toVector3F(),
        controls.getTarget().toVector3F(),
        camera.zoom.toDouble(),
        controls.getFocalOffset().toVector3F(),
        camera.rotation.z.toDouble()
    )

fun CameraPosition.update(camera: PerspectiveCamera, controls: CameraControls) {
    camera.setZRotation(zRotation)

    controls.setLookAt(
        position.x, position.y, position.z,
        target.x, target.y, target.z,
        true
    )
    controls.setFocalOffset(focalOffset.x, focalOffset.y, focalOffset.z, true)
}

fun PerspectiveCamera.setZRotation(angle: Double) {
    up.set(0, 1, 0)
    val cameraAngle = Matrix4()
    val rotated = cameraAngle.makeRotationZ(angle)
    up.applyMatrix4(rotated)
}