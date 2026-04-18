$BASE = "http://localhost:8080/api/v1"

function Invoke-Api {
    param($Method, $Url, $Body, $Token)
    $headers = @{}
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    try {
        if ($Body) {
            return Invoke-RestMethod -Uri "$BASE$Url" -Method $Method `
                -ContentType "application/json" `
                -Body ($Body | ConvertTo-Json -Depth 5) `
                -Headers $headers
        } else {
            return Invoke-RestMethod -Uri "$BASE$Url" -Method $Method -Headers $headers
        }
    } catch {
        $err = $_.ErrorDetails.Message
        if ($err) {
            try { $j = $err | ConvertFrom-Json; Write-Host "  [skip] $($j.message)" -ForegroundColor DarkYellow }
            catch { Write-Host "  [skip] $($_.Exception.Message)" -ForegroundColor DarkYellow }
        }
        return $null
    }
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  MediCare Data Seeder" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# ---------- ADMIN ----------
Write-Host "`n[1/4] Admin..." -ForegroundColor White
$b = [ordered]@{ name="Super Admin"; email="admin@medicare.com"; password="Admin@1234"; role="ROLE_ADMIN" }
$r = Invoke-Api -Method POST -Url "/auth/register" -Body $b
if (-not $r) { $r = Invoke-Api -Method POST -Url "/auth/login" -Body @{ email="admin@medicare.com"; password="Admin@1234" } }
$adminToken = $r.data.accessToken
if (-not $adminToken) { Write-Host "Cannot connect to backend. Is it running on port 8080?" -ForegroundColor Red; exit 1 }
Write-Host "  Admin ready" -ForegroundColor Green

# ---------- DOCTORS ----------
Write-Host "`n[2/4] Seeding 10 doctors..." -ForegroundColor White

$docData = @(
    "Dr. Arjun Sharma|arjun.sharma@medicare.com|Cardiology|08:00|16:00",
    "Dr. Priya Nair|priya.nair@medicare.com|Neurology|09:00|17:00",
    "Dr. Rahul Verma|rahul.verma@medicare.com|Orthopedics|10:00|18:00",
    "Dr. Sneha Iyer|sneha.iyer@medicare.com|Pediatrics|08:30|14:30",
    "Dr. Kiran Patel|kiran.patel@medicare.com|Dermatology|11:00|19:00",
    "Dr. Meera Reddy|meera.reddy@medicare.com|Gynecology|09:00|15:00",
    "Dr. Suresh Kumar|suresh.kumar@medicare.com|General Surgery|07:00|15:00",
    "Dr. Ananya Ghosh|ananya.ghosh@medicare.com|Psychiatry|10:00|17:00",
    "Dr. Vikram Singh|vikram.singh@medicare.com|Ophthalmology|08:00|13:00",
    "Dr. Lakshmi Pillai|lakshmi.pillai@medicare.com|ENT|09:30|17:30"
)

$doctorIds = @()
foreach ($line in $docData) {
    $parts = $line -split "\|"
    $dName = $parts[0]; $dEmail = $parts[1]; $dSpec = $parts[2]; $dFrom = $parts[3]; $dTo = $parts[4]

    $rb = @{ name=$dName; email=$dEmail; password="Doctor@1234"; role="ROLE_DOCTOR" }
    $rr = Invoke-Api -Method POST -Url "/auth/register" -Body $rb
    if (-not $rr) { $rr = Invoke-Api -Method POST -Url "/auth/login" -Body @{ email=$dEmail; password="Doctor@1234" } }
    if (-not $rr) { continue }

    $dToken = $rr.data.accessToken
    $dUid   = $rr.data.userId

    $pb = @{ specialization=$dSpec; availableFrom=$dFrom; availableTo=$dTo }
    $pr = Invoke-Api -Method POST -Url "/doctors/user/$dUid" -Body $pb -Token $dToken
    if ($pr) {
        $doctorIds += $pr.data.id
        Write-Host "  + $dName [$dSpec] id=$($pr.data.id)" -ForegroundColor Green
    } else {
        # Profile already exists, fetch all doctors to find id
        $all = Invoke-Api -Method GET -Url "/doctors" -Token $adminToken
        if ($all) {
            $match = $all.data | Where-Object { $_.email -eq $dEmail }
            if ($match) { $doctorIds += $match.id; Write-Host "  ~ $dName (existing)" -ForegroundColor Yellow }
        }
    }
}
Write-Host "  Doctors ready: $($doctorIds.Count)" -ForegroundColor Cyan

# ---------- PATIENTS ----------
Write-Host "`n[3/4] Seeding 50 patients..." -ForegroundColor White

$patData = @(
    "Aarav Mehta|aarav.mehta@gmail.com|28|Mild hypertension",
    "Aditi Sharma|aditi.sharma@gmail.com|34|Type 2 diabetes",
    "Ajay Nair|ajay.nair@gmail.com|45|Asthma, seasonal allergies",
    "Akash Patel|akash.patel@gmail.com|22|No significant history",
    "Ananya Singh|ananya.singh@gmail.com|31|Migraine headaches",
    "Anil Reddy|anil.reddy@gmail.com|55|Coronary artery disease",
    "Anjali Verma|anjali.verma@gmail.com|29|Hypothyroidism",
    "Arjun Gupta|arjun.gupta@gmail.com|38|Lower back pain",
    "Arun Kumar|arun.kumar@gmail.com|62|Type 2 diabetes, hypertension",
    "Ashok Iyer|ashok.iyer@gmail.com|47|GERD, obesity",
    "Bhavya Joshi|bhavya.joshi@gmail.com|25|Anxiety disorder",
    "Deepa Krishnan|deepa.krishnan@gmail.com|40|Rheumatoid arthritis",
    "Dhruv Malhotra|dhruv.malhotra@gmail.com|33|Eczema",
    "Divya Nair|divya.nair@gmail.com|27|PCOS",
    "Gaurav Sharma|gaurav.sharma@gmail.com|52|Chronic kidney disease stage 2",
    "Geeta Pillai|geeta.pillai@gmail.com|65|Osteoporosis, arthritis",
    "Hari Prasad|hari.prasad@gmail.com|43|Hypertension",
    "Ishaan Tiwari|ishaan.tiwari@gmail.com|19|No significant history",
    "Jaya Menon|jaya.menon@gmail.com|36|Depression, anxiety",
    "Karthik Rao|karthik.rao@gmail.com|49|Prostate issues",
    "Kavita Sharma|kavita.sharma@gmail.com|38|Migraine, sinusitis",
    "Keerthi Bhat|keerthi.bhat@gmail.com|24|No significant history",
    "Krishnan Iyer|krishnan.iyer@gmail.com|70|Heart failure, diabetes",
    "Laila Khan|laila.khan@gmail.com|32|Fibromyalgia",
    "Latha Reddy|latha.reddy@gmail.com|58|Hypothyroidism, hypertension",
    "Manoj Verma|manoj.verma@gmail.com|41|Fatty liver, obesity",
    "Maya Pillai|maya.pillai@gmail.com|30|Endometriosis",
    "Mohammed Iqbal|mohammed.iqbal@gmail.com|37|Asthma",
    "Nandita Sen|nandita.sen@gmail.com|44|Psoriasis",
    "Naveen Gowda|naveen.gowda@gmail.com|26|Sports injury - ACL tear",
    "Neha Jain|neha.jain@gmail.com|35|PCOS, hypothyroidism",
    "Nikhil Bhatt|nikhil.bhatt@gmail.com|29|No significant history",
    "Pallavi Shetty|pallavi.shetty@gmail.com|53|Breast cancer survivor",
    "Pooja Desai|pooja.desai@gmail.com|21|Acne, eczema",
    "Pradeep Nair|pradeep.nair@gmail.com|60|COPD, ex-smoker",
    "Priya Ghosh|priya.ghosh@gmail.com|28|Anxiety, insomnia",
    "Rahul Choudhary|rahul.choudhary@gmail.com|46|Hypertension, high cholesterol",
    "Rajesh Kumar|rajesh.kumar@gmail.com|67|Parkinson disease early stage",
    "Ritu Agarwal|ritu.agarwal@gmail.com|33|Celiac disease",
    "Rohit Saxena|rohit.saxena@gmail.com|39|Herniated disc",
    "Rupali Patil|rupali.patil@gmail.com|42|Thyroid nodule",
    "Sahil Mehta|sahil.mehta@gmail.com|23|No significant history",
    "Sanjay Dubey|sanjay.dubey@gmail.com|56|Diabetes type 1",
    "Santosh Rao|santosh.rao@gmail.com|48|Chronic sinusitis",
    "Seema Tiwari|seema.tiwari@gmail.com|31|Iron deficiency anemia",
    "Shreya Bajaj|shreya.bajaj@gmail.com|27|Polycystic kidney disease",
    "Shravya Shetty|shravya@gmail.com|22|No significant history",
    "Sunita Menon|sunita.menon@gmail.com|50|Menopause, osteoporosis",
    "Supriya Rao|supriya.rao@gmail.com|36|Lupus",
    "Vijay Shetty|vijay.shetty@gmail.com|64|Atrial fibrillation"
)

$patientTokens = @()
foreach ($line in $patData) {
    $parts  = $line -split "\|"
    $pName  = $parts[0]; $pEmail = $parts[1]; $pAge = [int]$parts[2]; $pHist = $parts[3]

    $rb = @{ name=$pName; email=$pEmail; password="Patient@1234"; role="ROLE_PATIENT" }
    $rr = Invoke-Api -Method POST -Url "/auth/register" -Body $rb
    if (-not $rr) { $rr = Invoke-Api -Method POST -Url "/auth/login" -Body @{ email=$pEmail; password="Patient@1234" } }
    if (-not $rr) { continue }

    $pToken = $rr.data.accessToken
    $pUid   = $rr.data.userId

    $pb = @{ age=$pAge; medicalHistory=$pHist }
    $pr = Invoke-Api -Method POST -Url "/patients/user/$pUid" -Body $pb -Token $pToken
    if ($pr) {
        Write-Host "  + $pName (age $pAge)" -ForegroundColor Green
    } else {
        Write-Host "  ~ $pName (profile exists)" -ForegroundColor Yellow
    }
    $patientTokens += $pToken
}
Write-Host "  Patients ready: $($patientTokens.Count)" -ForegroundColor Cyan

# ---------- APPOINTMENTS ----------
Write-Host "`n[4/4] Booking appointments..." -ForegroundColor White

$slots = @(
    "2026-04-10|09:00|COMPLETED", "2026-04-10|10:00|COMPLETED", "2026-04-10|11:00|COMPLETED",
    "2026-04-11|09:00|COMPLETED", "2026-04-11|10:30|COMPLETED", "2026-04-11|14:00|CANCELLED",
    "2026-04-12|09:00|NO_SHOW",   "2026-04-12|11:00|COMPLETED", "2026-04-13|09:00|CANCELLED",
    "2026-04-14|10:00|COMPLETED", "2026-04-15|11:00|COMPLETED", "2026-04-15|14:00|NO_SHOW",
    "2026-04-19|09:30|CONFIRMED", "2026-04-19|11:00|CONFIRMED", "2026-04-19|14:00|CONFIRMED",
    "2026-04-20|09:00|PENDING",   "2026-04-20|10:00|PENDING",   "2026-04-20|12:00|PENDING",
    "2026-04-21|09:00|PENDING",   "2026-04-21|11:00|PENDING",   "2026-04-21|14:00|PENDING",
    "2026-04-22|09:30|PENDING",   "2026-04-22|11:00|PENDING",   "2026-04-22|14:00|PENDING",
    "2026-04-23|09:00|PENDING",   "2026-04-23|11:00|PENDING",   "2026-04-23|15:00|PENDING",
    "2026-04-24|09:00|PENDING",   "2026-04-24|10:00|PENDING",   "2026-04-24|14:00|PENDING",
    "2026-04-25|08:30|PENDING",   "2026-04-25|10:00|PENDING",   "2026-04-25|12:00|PENDING",
    "2026-04-26|09:00|PENDING",   "2026-04-26|11:00|PENDING",   "2026-04-26|15:00|PENDING",
    "2026-04-28|09:00|PENDING",   "2026-04-28|10:30|PENDING",   "2026-04-28|14:00|PENDING",
    "2026-04-29|09:00|PENDING",   "2026-04-29|11:00|PENDING",   "2026-04-29|14:00|PENDING",
    "2026-04-30|09:30|PENDING",   "2026-04-30|11:00|PENDING",   "2026-04-30|14:00|PENDING"
)

$apptCount = 0
$skipCount = 0
$rand = [System.Random]::new(42)
$usedSlots = @{}

foreach ($pt in $patientTokens) {
    $numAppts = $rand.Next(1, 3)
    for ($i = 0; $i -lt $numAppts; $i++) {
        $booked = $false
        for ($t = 0; $t -lt 15; $t++) {
            $docId   = $doctorIds[$rand.Next(0, $doctorIds.Count)]
            $slotLine = $slots[$rand.Next(0, $slots.Count)]
            $sp       = $slotLine -split "\|"
            $slotDate = $sp[0]; $slotTime = $sp[1]; $slotStatus = $sp[2]
            $key = "$docId|$slotDate|$slotTime"
            if ($usedSlots[$key]) { continue }
            $usedSlots[$key] = $true

            $ab = @{ doctorId=$docId; date=$slotDate; time=$slotTime }
            $ar = Invoke-Api -Method POST -Url "/appointments" -Body $ab -Token $pt
            if ($ar -and $ar.data.id) {
                $aid = $ar.data.id
                if ($slotStatus -ne "PENDING") {
                    Invoke-RestMethod -Uri "$BASE/appointments/$aid/status?status=$slotStatus" `
                        -Method PATCH `
                        -Headers @{ Authorization = "Bearer $adminToken" } `
                        -ErrorAction SilentlyContinue | Out-Null
                }
                $apptCount++
                $booked = $true
                break
            }
        }
        if (-not $booked) { $skipCount++ }
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  SEEDING COMPLETE!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host "  Doctors      : $($doctorIds.Count)"
Write-Host "  Patients     : $($patientTokens.Count)"
Write-Host "  Appointments : $apptCount booked, $skipCount skipped"
Write-Host ""
Write-Host "Login credentials:" -ForegroundColor Yellow
Write-Host "  Admin   -> admin@medicare.com        / Admin@1234"
Write-Host "  Doctor  -> arjun.sharma@medicare.com / Doctor@1234"
Write-Host "  Patient -> shravya@gmail.com         / Patient@1234"
Write-Host ""
