$ErrorActionPreference = 'Stop'
$key = 'C:\Users\User\Downloads\Rajneeti_key.pem'
$pf  = 'D:\rajneeti\evidence_fa_statistics.pdf'
Remove-Item -LiteralPath $pf -Force -ErrorAction SilentlyContinue

scp -i $key -o BatchMode=yes -o StrictHostKeyChecking=accept-new -o ConnectTimeout=25 `
  rajneeti@52.175.124.91:/tmp/rajn_stat_fa.pdf $pf *> 'D:\rajneeti\qa_retrieve.log' 2>&1
Write-Output "RETRIEVE_EXIT=$LASTEXITCODE"

if (Test-Path -LiteralPath $pf) {
  $b = [System.IO.File]::ReadAllBytes($pf)
  Write-Output ("LOCAL_BYTECOUNT=" + $b.Length)
  $head = [System.Text.Encoding]::ASCII.GetString($b[0..7])
  Write-Output "LOCAL_HEAD8=$head"
  $n = $b.Length
  $tail = [System.Text.Encoding]::ASCII.GetString($b[($n-16)..($n-1)])
  Write-Output "LOCAL_TAIL16=$tail"
  $all = [System.Text.Encoding]::ASCII.GetString($b)
  Write-Output ("LOCAL_HAS_PCTEOF=" + $all.Contains('%%EOF'))
  Write-Output ("LOCAL_HAS_PRODUCER_OpenPDF=" + $all.Contains('OpenPDF'))
}
