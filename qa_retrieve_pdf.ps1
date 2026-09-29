$ErrorActionPreference = 'Stop'
$key = 'C:\Users\User\Downloads\Rajneeti_key.pem'
$evidence = 'D:\rajneeti\evidence_fa_statistics.pdf'
Remove-Item -LiteralPath $evidence -Force -ErrorAction SilentlyContinue
scp -i $key -o BatchMode=yes -o StrictHostKeyChecking=accept-new -o ConnectTimeout=25 rajneeti@52.175.124.91:/tmp/rajn_stat_fa.pdf $evidence *> 'D:\rajneeti\qa_retrieve.log' 2>&1
Write-Output ("RETRIEVE_EXIT=" + $LASTEXITCODE)
if (Test-Path -LiteralPath $evidence) {
  $b = [System.IO.File]::ReadAllBytes($evidence)
  Write-Output ("EVIDENCE_BYTES=" + $b.Length)
  $headTxt = [System.Text.Encoding]::ASCII.GetString($b[0..7])
  Write-Output ("EVIDENCE_HEAD8=" + $headTxt)
  $tailTxt = [System.Text.Encoding]::ASCII.GetString($b[($b.Length-16)..($b.Length-1)])
  Write-Output ("EVIDENCE_TAIL16=" + $tailTxt.Replace("`r",'<CR>').Replace("`n",'<LF>'))
  $raw = [System.Text.Encoding]::ASCII.GetString($b)
  Write-Output ("HAS_PCTEOF=" + $raw.Contains("%%EOF"))
  Write-Output ("HAS_STARTXREF=" + $raw.Contains("startxref"))
  Write-Output ("HAS_OpenPDF=" + $raw.Contains("OpenPDF"))
  Write-Output ("HAS_RAJNEETI_TITLE=" + ($raw -match "/Title\s*\([^)]*[Rr]ajneeti[^)]*\)"))
}
Write-Output "====RETRIEVE_DONE===="
