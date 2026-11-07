$ErrorActionPreference='Stop'
$bioAdb='C:/Users/User/AppData/Local/Android/Sdk/platform-tools/adb.exe'
$bioRun=Start-Process -FilePath $bioAdb -ArgumentList '-s','emulator-5556','shell','am','instrument','-w','com.alvia.health.test/com.alvia.health.DeletionTests' -WindowStyle Hidden -RedirectStandardOutput '.tools/deletion-device.log' -RedirectStandardError '.tools/deletion-device-error.log' -PassThru
$bioUntil=(Get-Date).AddSeconds(150)
while((Get-Date) -lt $bioUntil -and -not $bioRun.HasExited){
    $bioLog=Get-Content -Raw '.tools/deletion-device.log' -ErrorAction SilentlyContinue
    if($bioLog -match 'READY_TO_DELETE'){
        & $bioAdb -s emulator-5556 pull /sdcard/Android/data/com.alvia.health/files/deletion-light.png docs/deletion-light.png
        & $bioAdb -s emulator-5556 pull /sdcard/Android/data/com.alvia.health/files/deletion-dark.png docs/deletion-dark.png
        break
    }
    Start-Sleep -Seconds 1
    $bioRun.Refresh()
}
$bioRun.WaitForExit(120000) | Out-Null
Get-Content '.tools/deletion-device.log'
Get-Content '.tools/deletion-device-error.log'
& $bioAdb -s emulator-5556 shell run-as com.alvia.health ls files
