@echo off
REM ---------------------------------------------------------------------------
REM Chay ung dung o moi truong phat trien.
REM
REM May nay dang co JAVA_HOME tro vao jdk-17, nhung do an yeu cau Java 21
REM (DOAN.md muc 3), nen script nay tu tro sang jdk-21 trong pham vi phien lam viec.
REM
REM Cach lam dung ve lau dai: sua bien moi truong JAVA_HOME cua he thong thanh
REM jdk-21, roi xoa file nay va chay truc tiep:  mvn spring-boot:run
REM
REM Neu duong dan JDK 21 tren may ban khac, sua dong SET JAVA_HOME ben duoi.
REM ---------------------------------------------------------------------------

if not exist "C:\Program Files\Java\jdk-21" (
    echo [LOI] Khong tim thay JDK 21 o C:\Program Files\Java\jdk-21
    echo        Sua duong dan trong file start-dev.cmd cho dung may cua ban.
    exit /b 1
)

set "JAVA_HOME=C:\Program Files\Java\jdk-21"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo Dang bat MySQL...
docker compose up -d
if errorlevel 1 (
    echo [LOI] Khong bat duoc MySQL. Kiem tra Docker Desktop da chay chua.
    exit /b 1
)

REM Cho MySQL bao healthy. Lan dau tao volume co the mat 30-60 giay; neu bo qua
REM buoc cho nay thi Flyway se chet voi "Communications link failure".
echo Dang cho MySQL san sang...
for /L %%i in (1,1,60) do (
    for /f "delims=" %%s in ('docker inspect -f "{{.State.Health.Status}}" ptit-course-mysql 2^>nul') do (
        if "%%s"=="healthy" goto :ready
    )
    timeout /t 3 /nobreak > nul
)
echo [LOI] MySQL khong san sang sau 3 phut. Xem log:  docker compose logs mysql
exit /b 1

:ready
echo MySQL da san sang.
echo Dang dung Java tai: %JAVA_HOME%
echo Ung dung se chay o http://localhost:8080
echo.

mvn spring-boot:run
