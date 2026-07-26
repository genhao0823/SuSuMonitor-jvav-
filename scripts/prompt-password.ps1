param(
    [Parameter(Mandatory=$true)][string]$Prompt
)

$secure = Read-Host -AsSecureString -Prompt $Prompt
$ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try {
    [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
}
