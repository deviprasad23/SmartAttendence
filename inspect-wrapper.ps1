Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead('.mvn\\wrapper\\maven-wrapper.jar')
$zip.Entries | Select-Object -First 20 | ForEach-Object { $_.FullName }
$zip.Dispose()
