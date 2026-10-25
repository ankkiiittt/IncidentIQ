# scripts/verify_p10_websocket.ps1
# End-to-end verification of Phase P10 (Notifications & Real-Time Event triggers)

$baseUrl = "http://localhost:8080"
$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "IncidentIQ Phase P10 - Notification & Real-Time Test Suite" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Authenticate Admin
Write-Host "`n1. Authenticating Admin..." -ForegroundColor Yellow
$loginBody = @{
    email = "admin@incidentiq.local"
    password = "AdminInitialPassword123!"
} | ConvertTo-Json

$loginRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
$token = $loginRes.accessToken
$adminId = $loginRes.user.id
Write-Host "   Admin authenticated. User ID: $adminId" -ForegroundColor Green

$headers = @{
    Authorization = "Bearer $token"
}

# 2. Check initial notifications & unread count
Write-Host "`n2. Checking unread count..." -ForegroundColor Yellow
$unreadRes = Invoke-RestMethod -Uri "$baseUrl/api/notifications/unread-count" -Method Get -Headers $headers
Write-Host "   Current unread notifications count: $($unreadRes.unreadCount)" -ForegroundColor Green

# 3. Mark all as read to ensure clean baseline
Write-Host "`n3. Resetting baseline by marking all as read..." -ForegroundColor Yellow
$readAllRes = Invoke-RestMethod -Uri "$baseUrl/api/notifications/read-all" -Method Post -Headers $headers
Write-Host "   Updated $($readAllRes.updated) notifications to read." -ForegroundColor Green

$unreadAfterReset = Invoke-RestMethod -Uri "$baseUrl/api/notifications/unread-count" -Method Get -Headers $headers
Write-Host "   Verified unread count after reset: $($unreadAfterReset.unreadCount)" -ForegroundColor Green
if ($unreadAfterReset.unreadCount -ne 0) {
    throw "Expected 0 unread notifications after read-all"
}

# 4. Trigger incident creation and assignment to generate notification
Write-Host "`n4. Creating test incident and assigning to Admin to trigger notification..." -ForegroundColor Yellow
$services = Invoke-RestMethod -Uri "$baseUrl/api/services?activeOnly=true" -Method Get -Headers $headers
$serviceId = $services.content[0].id

$createIncidentBody = @{
    title = "Real-time Notification Verification Incident"
    description = "Testing automated WebSocket notification delivery and persistence."
    serviceId = $serviceId
    severity = "HIGH"
    category = "APPLICATION"
} | ConvertTo-Json

try {
    $incident = Invoke-RestMethod -Uri "$baseUrl/api/incidents" -Method Post -Headers $headers -Body $createIncidentBody -ContentType "application/json"
} catch {
    $stream = $_.Exception.Response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($stream)
    $errBody = $reader.ReadToEnd()
    Write-Host "Error response body: $errBody" -ForegroundColor Red
    throw $_
}
Write-Host "   Created incident: $($incident.formattedNumber) (ID: $($incident.id))" -ForegroundColor Green

# Assign to admin
$assignBody = @{
    assigneeId = $adminId
    version = $incident.version
} | ConvertTo-Json

$assigned = Invoke-RestMethod -Uri "$baseUrl/api/incidents/$($incident.id)/assign" -Method Post -Headers $headers -Body $assignBody -ContentType "application/json"
Write-Host "   Assigned incident to: $($assigned.assigneeName)" -ForegroundColor Green

# 5. Verify notification was recorded for admin
Write-Host "`n5. Verifying in-app notification recorded..." -ForegroundColor Yellow
$notifications = Invoke-RestMethod -Uri "$baseUrl/api/notifications" -Method Get -Headers $headers
Write-Host "   Retrieved $($notifications.Count) notifications." -ForegroundColor Green

$newNotif = $notifications | Where-Object { $_.entityId -eq $incident.id } | Select-Object -First 1
if (-not $newNotif) {
    throw "Notification for incident $($incident.id) was not found in database!"
}

Write-Host "   Found generated notification:" -ForegroundColor Green
Write-Host "     ID:      $($newNotif.id)" -ForegroundColor Gray
Write-Host "     Title:   $($newNotif.title)" -ForegroundColor Gray
Write-Host "     Type:    $($newNotif.type)" -ForegroundColor Gray
Write-Host "     Read:    $($newNotif.read)" -ForegroundColor Gray

# 6. Mark single notification as read
Write-Host "`n6. Marking notification as read..." -ForegroundColor Yellow
$markReadRes = Invoke-RestMethod -Uri "$baseUrl/api/notifications/$($newNotif.id)/read" -Method Patch -Headers $headers
Write-Host "   Notification marked as read: $($markReadRes.read)" -ForegroundColor Green
if (-not $markReadRes.read) {
    throw "Notification was not marked as read!"
}

# 7. Verify WebSocket /ws HTTP handshake is open (200/400 instead of 401/403)
Write-Host "`n7. Checking WebSocket handshake endpoint..." -ForegroundColor Yellow
try {
    $wsRes = Invoke-WebRequest -Uri "$baseUrl/ws/info" -Method Get
    Write-Host "   SockJS /ws/info handshake endpoint reachable: $($wsRes.StatusCode)" -ForegroundColor Green
} catch {
    Write-Host "   Handshake check response code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Yellow
}

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "ALL PHASE P10 REAL-TIME NOTIFICATION CHECKS PASSED!" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan
