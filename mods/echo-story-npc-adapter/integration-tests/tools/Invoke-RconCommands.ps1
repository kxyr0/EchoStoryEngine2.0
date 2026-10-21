param(
    [string] $HostName = '127.0.0.1',
    [int] $Port,
    [string] $Password,
    [string[]] $Commands
)

$ErrorActionPreference = 'Stop'
$TextEncoding = [System.Text.Encoding]::UTF8

function Read-Exact([System.IO.Stream] $Stream, [int] $Count) {
    $Buffer = [byte[]]::new($Count)
    $Offset = 0
    while ($Offset -lt $Count) {
        $Read = $Stream.Read($Buffer, $Offset, $Count - $Offset)
        if ($Read -le 0) {
            throw 'RCON stream closed.'
        }
        $Offset += $Read
    }
    return $Buffer
}

function Send-Packet([System.IO.Stream] $Stream, [int] $Id, [int] $Type, [string] $Payload) {
    $Body = $TextEncoding.GetBytes($Payload)
    $Length = 4 + 4 + $Body.Length + 2
    $Packet = [byte[]]::new(4 + $Length)
    [BitConverter]::GetBytes([int] $Length).CopyTo($Packet, 0)
    [BitConverter]::GetBytes([int] $Id).CopyTo($Packet, 4)
    [BitConverter]::GetBytes([int] $Type).CopyTo($Packet, 8)
    [Array]::Copy($Body, 0, $Packet, 12, $Body.Length)
    $Stream.Write($Packet, 0, $Packet.Length)
}

function Read-Packet([System.IO.Stream] $Stream) {
    $LengthBytes = Read-Exact $Stream 4
    $Length = [BitConverter]::ToInt32($LengthBytes, 0)
    $Data = Read-Exact $Stream $Length
    $Id = [BitConverter]::ToInt32($Data, 0)
    $PayloadLength = [Math]::Max(0, $Length - 10)
    $Payload = $TextEncoding.GetString($Data, 8, $PayloadLength)
    [pscustomobject]@{ Id = $Id; Payload = $Payload }
}

if ($Port -le 0) {
    throw 'RCON port is required.'
}
if ([string]::IsNullOrWhiteSpace($Password)) {
    throw 'RCON password is required.'
}
if ($null -eq $Commands -or $Commands.Count -eq 0) {
    throw 'At least one RCON command is required.'
}

$Client = [System.Net.Sockets.TcpClient]::new($HostName, $Port)
$Stream = $Client.GetStream()

try {
    Send-Packet $Stream 10 3 $Password
    $Auth = Read-Packet $Stream
    if ($Auth.Id -ne 10) {
        throw "RCON auth failed: id=$($Auth.Id)."
    }

    $Id = 20
    foreach ($Command in $Commands) {
        if ($Command.StartsWith('sleep:', [System.StringComparison]::OrdinalIgnoreCase)) {
            $Seconds = [int] $Command.Substring('sleep:'.Length)
            Start-Sleep -Seconds $Seconds
            continue
        }
        Send-Packet $Stream $Id 2 $Command
        $Response = Read-Packet $Stream
        Write-Output "--- $Command ---"
        Write-Output $Response.Payload
        $Id += 1
    }
} finally {
    $Stream.Dispose()
    $Client.Dispose()
}
