Get-ChildItem -Recurse -Filter "*.java" -Path "D:\项目\Armavoke" | Select-Object -ExpandProperty FullName | Out-File -FilePath "D:\项目\Armavoke\_javafiles.txt" -Encoding UTF8
