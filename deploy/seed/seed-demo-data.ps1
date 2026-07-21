param(
    [string]$ApiBase = "http://localhost:8080/api",
    [string]$Username = "demo_hr",
    [string]$Password = "Demo@123",
    [switch]$ReindexVectors
)

$ErrorActionPreference = "Stop"
$seedRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

function Invoke-JsonApi {
    param(
        [ValidateSet("GET", "POST", "PUT", "PATCH", "DELETE")]
        [string]$Method,
        [string]$Path,
        $Body = $null,
        [hashtable]$Headers = @{}
    )

    $parameters = @{
        Method      = $Method
        Uri         = "$ApiBase$Path"
        Headers     = $Headers
        ContentType = "application/json; charset=utf-8"
    }
    if ($null -ne $Body) {
        $json = $Body | ConvertTo-Json -Depth 20
        $parameters.Body = [System.Text.Encoding]::UTF8.GetBytes($json)
    }
    $parameters.UseBasicParsing = $true
    try {
        $response = Invoke-WebRequest @parameters
    }
    catch {
        $response = $_.Exception.Response
        if ($null -ne $response) {
            $reader = [System.IO.StreamReader]::new($response.GetResponseStream(), [System.Text.Encoding]::UTF8)
            $errorBody = $reader.ReadToEnd()
            $reader.Dispose()
            throw "$Method $Path failed: $errorBody"
        }
        throw
    }
    if ($response.StatusCode -eq 204 -or $response.RawContentLength -eq 0) {
        return $null
    }
    $stream = $response.RawContentStream
    $stream.Position = 0
    $bytes = New-Object byte[] $stream.Length
    [void]$stream.Read($bytes, 0, $bytes.Length)
    $text = [System.Text.Encoding]::UTF8.GetString($bytes)
    $parsed = $text | ConvertFrom-Json
    if ($parsed -is [System.Array]) {
        foreach ($item in $parsed) {
            Write-Output $item
        }
        return
    }
    $parsed
}

function Upload-Resume {
    param(
        [long]$CandidateId,
        [string]$FilePath,
        [string]$Token
    )

    Add-Type -AssemblyName System.Net.Http
    $client = [System.Net.Http.HttpClient]::new()
    $client.DefaultRequestHeaders.Authorization =
        [System.Net.Http.Headers.AuthenticationHeaderValue]::new("Bearer", $Token)
    $multipart = [System.Net.Http.MultipartFormDataContent]::new()
    try {
        $bytes = [System.IO.File]::ReadAllBytes($FilePath)
        $fileContent = [System.Net.Http.ByteArrayContent]::new($bytes)
        $fileContent.Headers.ContentType =
            [System.Net.Http.Headers.MediaTypeHeaderValue]::new("text/plain")
        $multipart.Add($fileContent, "file", [System.IO.Path]::GetFileName($FilePath))
        $response = $client.PostAsync(
            "$ApiBase/resumes/upload?candidateId=$CandidateId",
            $multipart
        ).GetAwaiter().GetResult()
        $response.EnsureSuccessStatusCode()
        $response.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json
    }
    finally {
        $multipart.Dispose()
        $client.Dispose()
    }
}

function Upload-KnowledgeDocument {
    param(
        [string]$FilePath,
        [string]$Token
    )

    Add-Type -AssemblyName System.Net.Http
    $client = [System.Net.Http.HttpClient]::new()
    $client.DefaultRequestHeaders.Authorization =
        [System.Net.Http.Headers.AuthenticationHeaderValue]::new("Bearer", $Token)
    $multipart = [System.Net.Http.MultipartFormDataContent]::new()
    try {
        $bytes = [System.IO.File]::ReadAllBytes($FilePath)
        $fileContent = [System.Net.Http.ByteArrayContent]::new($bytes)
        $fileContent.Headers.ContentType =
            [System.Net.Http.Headers.MediaTypeHeaderValue]::new("text/markdown")
        $multipart.Add($fileContent, "file", [System.IO.Path]::GetFileName($FilePath))
        $response = $client.PostAsync(
            "$ApiBase/knowledge/documents/upload",
            $multipart
        ).GetAwaiter().GetResult()
        $response.EnsureSuccessStatusCode()
        $response.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json
    }
    finally {
        $multipart.Dispose()
        $client.Dispose()
    }
}

Write-Host "Checking backend: $ApiBase" -ForegroundColor Cyan
try {
    Invoke-JsonApi -Method POST -Path "/auth/register" -Body @{
        username = $Username
        password = $Password
    } | Out-Null
    Write-Host "Created demo account: $Username" -ForegroundColor Green
}
catch {
    Write-Host "Demo account already exists, continuing with login." -ForegroundColor DarkYellow
}

$login = Invoke-JsonApi -Method POST -Path "/auth/login" -Body @{
    username = $Username
    password = $Password
}
$token = $login.accessToken
$headers = @{ Authorization = "Bearer $token" }

$jobDefinitions = @(
    @{
        Key = "java"
        Name = "Java 后端开发工程师"
        Description = "负责订单、用户和营销等核心服务设计与开发；要求熟悉 Java、Spring Boot、MySQL，能够处理缓存一致性、消息幂等、接口性能和线上故障。具备 Redis、Kafka、Docker 或微服务经验优先。"
        RequiredSkills = @("Java", "Spring Boot", "MySQL")
        PreferredSkills = @("Redis", "Kafka", "Docker", "Spring Cloud")
        Education = "BACHELOR"
        MinYears = 2
        MaxYears = 6
    },
    @{
        Key = "frontend"
        Name = "高级前端开发工程师"
        Description = "负责企业级 Web 应用、组件库和前端工程体系建设；要求熟悉 Vue 3、TypeScript、JavaScript，具备复杂交互、性能优化和工程化实践。"
        RequiredSkills = @("Vue", "TypeScript", "JavaScript")
        PreferredSkills = @("Vite", "Pinia", "ECharts", "Node.js")
        Education = "BACHELOR"
        MinYears = 3
        MaxYears = 8
    },
    @{
        Key = "data"
        Name = "数据分析师"
        Description = "负责经营指标体系、增长分析、用户分层和专题分析；要求熟练使用 SQL 和 Python，能够独立完成数据提取、分析建模、可视化和业务建议。"
        RequiredSkills = @("SQL", "Python", "统计分析")
        PreferredSkills = @("Tableau", "Airflow", "A/B测试", "Excel")
        Education = "BACHELOR"
        MinYears = 2
        MaxYears = 6
    },
    @{
        Key = "devops"
        Name = "DevOps 工程师"
        Description = "负责 Kubernetes 集群、持续交付、可观测性和云资源治理；要求熟悉 Linux、Docker、Kubernetes 和 CI/CD，具备 Prometheus、Grafana 或 Terraform 实践。"
        RequiredSkills = @("Linux", "Docker", "Kubernetes")
        PreferredSkills = @("Jenkins", "Prometheus", "Grafana", "Terraform")
        Education = "COLLEGE"
        MinYears = 2
        MaxYears = 7
    }
)

$jobs = @{}
$existingJobs = @(Invoke-JsonApi -Method GET -Path "/jobs" -Headers $headers)
foreach ($definition in $jobDefinitions) {
    $job = $existingJobs | Where-Object { $_.name -eq $definition.Name } | Select-Object -First 1
    $created = $false
    if ($null -eq $job) {
        $job = Invoke-JsonApi -Method POST -Path "/jobs" -Headers $headers -Body @{
            name = $definition.Name
            description = $definition.Description
            requiredSkills = $definition.RequiredSkills
            preferredSkills = $definition.PreferredSkills
            educationRequirement = $definition.Education
            minExperienceYears = $definition.MinYears
            maxExperienceYears = $definition.MaxYears
        }
        $created = $true
        Write-Host "Created job: $($definition.Name)" -ForegroundColor Green
    }
    $jobs[$definition.Key] = @{ Value = $job; Created = $created; Definition = $definition }
}

$candidateDefinitions = @(
    @{
        Key = "zhang"
        Name = "张伟"
        Education = "BACHELOR"
        Years = 6.5
        City = "上海"
        Skills = @("Java", "Spring Boot", "Spring Cloud", "MySQL", "Redis", "Kafka", "Docker", "Linux")
        Resume = "zhang-wei-java.txt"
        School = "华东理工大学"
        Major = "计算机科学与技术"
        Company = "云帆电商科技"
        Title = "高级 Java 开发工程师"
        Project = "电商订单中心"
        Role = "核心后端开发"
        ProjectDescription = "负责订单状态机、库存预占、Kafka 消息幂等、Redis 缓存和 MySQL 慢查询优化，大促峰值约 3200 QPS。"
    },
    @{
        Key = "li"
        Name = "李娜"
        Education = "BACHELOR"
        Years = 4.5
        City = "杭州"
        Skills = @("Java", "Spring Boot", "MySQL", "Redis", "MyBatis", "Git")
        Resume = "li-na-java.txt"
        School = "浙江工业大学"
        Major = "软件工程"
        Company = "杭州启明信息"
        Title = "Java 开发工程师"
        Project = "企业 CRM 平台"
        Role = "后端开发"
        ProjectDescription = "负责客户标签、数据权限、导入导出和千万级客户表查询优化。"
    },
    @{
        Key = "wang"
        Name = "王磊"
        Education = "BACHELOR"
        Years = 7.5
        City = "深圳"
        Skills = @("Vue", "TypeScript", "JavaScript", "Vite", "Pinia", "ECharts", "Node.js")
        Resume = "wang-lei-frontend.txt"
        School = "深圳大学"
        Major = "软件工程"
        Company = "星河零售"
        Title = "高级前端工程师"
        Project = "零售运营平台"
        Role = "前端负责人"
        ProjectDescription = "主导 Vue 3 组件库、权限路由和微前端接入，将首屏加载时间从 4.2 秒降低到 1.8 秒。"
    },
    @{
        Key = "chen"
        Name = "陈晨"
        Education = "MASTER"
        Years = 4.5
        City = "北京"
        Skills = @("SQL", "Python", "Tableau", "Excel", "Airflow", "统计分析", "A/B测试")
        Resume = "chen-chen-data.txt"
        School = "北京交通大学"
        Major = "应用统计"
        Company = "北辰出行"
        Title = "数据分析师"
        Project = "用户增长分析平台"
        Role = "数据分析负责人"
        ProjectDescription = "建立新增、留存、转化和复购指标体系，设计 A/B 测试并推动新客转化率提升 8.6%。"
    },
    @{
        Key = "zhao"
        Name = "赵敏"
        Education = "BACHELOR"
        Years = 8.5
        City = "上海"
        Skills = @("需求分析", "Axure", "流程设计", "用户研究", "数据分析", "项目管理")
        Resume = "zhao-min-product.txt"
        School = "南京大学"
        Major = "信息管理"
        Company = "云图协同"
        Title = "高级产品经理"
        Project = "企业流程平台"
        Role = "产品负责人"
        ProjectDescription = "负责客户访谈、流程建模、原型设计和上线复盘，需求交付周期缩短 30%。"
    },
    @{
        Key = "liu"
        Name = "刘洋"
        Education = "BACHELOR"
        Years = 5.5
        City = "成都"
        Skills = @("Kubernetes", "Docker", "Linux", "Jenkins", "Prometheus", "Grafana", "Terraform")
        Resume = "liu-yang-devops.txt"
        School = "电子科技大学"
        Major = "网络工程"
        Company = "成都智云"
        Title = "DevOps 工程师"
        Project = "云原生交付平台"
        Role = "平台工程师"
        ProjectDescription = "负责 Kubernetes、CI/CD 和监控告警，将服务平均发布时间从 45 分钟缩短到 12 分钟。"
    }
)

$candidates = @{}
$existingCandidates = @(Invoke-JsonApi -Method GET -Path "/candidates" -Headers $headers)
foreach ($definition in $candidateDefinitions) {
    $candidate = $existingCandidates | Where-Object { $_.name -eq $definition.Name } | Select-Object -First 1
    $created = $false
    if ($null -eq $candidate) {
        $candidate = Invoke-JsonApi -Method POST -Path "/candidates" -Headers $headers -Body @{
            name = $definition.Name
            phoneHash = "demo-phone-$($definition.Key)"
            emailHash = "demo-$($definition.Key)@example.invalid"
            educationLevel = $definition.Education
            totalExperienceYears = $definition.Years
            expectedCity = $definition.City
            skills = $definition.Skills
        }
        $created = $true
        Write-Host "Created candidate: $($definition.Name)" -ForegroundColor Green
    }

    $resumes = @(Invoke-JsonApi -Method GET -Path "/candidates/$($candidate.id)/resumes" -Headers $headers)
    $resume = $resumes | Where-Object { $_.originalFilename -eq $definition.Resume } | Select-Object -First 1
    if ($null -eq $resume) {
        $resumePath = Join-Path $seedRoot "resumes\$($definition.Resume)"
        $resume = Upload-Resume -CandidateId $candidate.id -FilePath $resumePath -Token $token
        Invoke-JsonApi -Method POST -Path "/resumes/$($resume.id)/extract-text" -Headers $headers | Out-Null
        Write-Host "Uploaded resume: $($definition.Resume)" -ForegroundColor Green
    }

    $detail = @{
        educationExperiences = @(@{
            school = $definition.School
            degree = $(if ($definition.Education -eq "MASTER") { "硕士" } else { "本科" })
            major = $definition.Major
            startDate = "2015-09"
            endDate = "2019-06"
            sourceText = "$($definition.School) $($definition.Major)"
            confidence = 0.96
        })
        workExperiences = @(@{
            company = $definition.Company
            title = $definition.Title
            startDate = "2021-01"
            endDate = "至今"
            description = "负责核心业务系统建设、项目交付和持续优化。"
            sourceText = "$($definition.Company) $($definition.Title)"
            confidence = 0.94
        })
        projectExperiences = @(@{
            name = $definition.Project
            role = $definition.Role
            startDate = "2022-01"
            endDate = "至今"
            description = $definition.ProjectDescription
            sourceText = $definition.ProjectDescription
            confidence = 0.95
        })
        certificates = @()
        languages = @("英语")
        awards = @()
        fieldSources = @{
            educationLevel = "$($definition.School) $($definition.Major)"
            totalExperienceYears = "$($definition.Years) 年相关经验"
            expectedCity = $definition.City
            skills = ($definition.Skills -join "、")
        }
        fieldConfidence = @{
            educationLevel = 0.96
            totalExperienceYears = 0.90
            expectedCity = 0.98
            skills = 0.92
        }
    }
    Invoke-JsonApi -Method PUT -Path "/resumes/$($resume.id)/structured-detail" -Headers $headers -Body $detail | Out-Null
    $candidates[$definition.Key] = @{
        Value = $candidate
        Resume = $resume
        Created = $created
        Definition = $definition
    }
}

foreach ($entry in $jobs.Values) {
    if ($entry.Created -or $ReindexVectors) {
        Invoke-JsonApi -Method POST -Path "/vector/jobs/$($entry.Value.id)/description" -Headers $headers -Body @{
            content = $entry.Value.description
        } | Out-Null
    }
}

foreach ($entry in $candidates.Values) {
    if ($entry.Created -or $ReindexVectors) {
        Invoke-JsonApi -Method POST -Path "/vector/candidates/$($entry.Value.id)/projects" -Headers $headers -Body @{
            projectId = 1
            content = "$($entry.Definition.Project)`n$($entry.Definition.Role)`n$($entry.Definition.ProjectDescription)"
        } | Out-Null
    }
}

$screeningPairs = @(
    @("java", "zhang"),
    @("java", "li"),
    @("java", "wang"),
    @("frontend", "wang"),
    @("frontend", "zhang"),
    @("data", "chen"),
    @("devops", "liu")
)
foreach ($pair in $screeningPairs) {
    $jobEntry = $jobs[$pair[0]]
    $candidateEntry = $candidates[$pair[1]]
    $rankings = @(Invoke-JsonApi -Method GET -Path "/jobs/$($jobEntry.Value.id)/candidates?candidateId=$($candidateEntry.Value.id)" -Headers $headers)
    if (-not ($rankings | Where-Object { $_.candidateId -eq $candidateEntry.Value.id })) {
        Invoke-JsonApi -Method POST -Path "/jobs/$($jobEntry.Value.id)/screen" -Headers $headers -Body @{
            candidateId = $candidateEntry.Value.id
        } | Out-Null
    }
}

$conversationPairs = @(
    @("java", "zhang", "ASK_AVAILABILITY"),
    @("frontend", "wang", "INITIAL_CONTACT"),
    @("data", "chen", "ASK_INTENTION")
)
foreach ($pair in $conversationPairs) {
    $job = $jobs[$pair[0]].Value
    $candidate = $candidates[$pair[1]].Value
    $existing = @(Invoke-JsonApi -Method GET -Path "/conversations?jobId=$($job.id)&candidateId=$($candidate.id)&limit=10" -Headers $headers)
    if ($existing.Count -eq 0) {
        $conversation = Invoke-JsonApi -Method POST -Path "/conversations" -Headers $headers -Body @{
            jobId = $job.id
            candidateId = $candidate.id
            stage = $pair[2]
        }
        Invoke-JsonApi -Method POST -Path "/conversations/$($conversation.id)/messages" -Headers $headers -Body @{
            senderType = "HR"
            content = "你好，我们看到你的项目经历与岗位比较匹配，想了解你近期是否考虑新的机会？"
        } | Out-Null
        Invoke-JsonApi -Method POST -Path "/conversations/$($conversation.id)/messages" -Headers $headers -Body @{
            senderType = "CANDIDATE"
            content = "你好，可以了解一下岗位负责的业务方向和面试安排吗？"
        } | Out-Null
    }
}

$knowledgeDocuments = @(Invoke-JsonApi -Method GET -Path "/knowledge/documents" -Headers $headers)
Get-ChildItem -LiteralPath (Join-Path $seedRoot "knowledge") -Filter "*.md" | ForEach-Object {
    $sourceName = $_.Name
    if (-not ($knowledgeDocuments | Where-Object { $_.sourceName -eq $sourceName })) {
        Upload-KnowledgeDocument -FilePath $_.FullName -Token $token | Out-Null
        Write-Host "Indexed RAG document: $sourceName" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "Demo data is ready." -ForegroundColor Cyan
Write-Host "Account: $Username / $Password"
Write-Host "MySQL data: jobs, candidates, resumes, evaluations, conversations and messages"
Write-Host "pgvector data: job vectors, resume project vectors and RAG knowledge chunks"
Write-Host "RAG source files: $seedRoot\knowledge"
