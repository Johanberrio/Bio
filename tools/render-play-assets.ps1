# Render store layouts from Bio's existing brand asset. The source logo and
# Android resources are read-only; no generated artwork or AI calls are used.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$projectRoot = Split-Path $PSScriptRoot -Parent
$outputFolder = Join-Path $projectRoot 'docs/play-assets'
New-Item -ItemType Directory -Path $outputFolder -Force | Out-Null
function Brush([string]$hex) { return [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml($hex)) }
function Label($g, [string]$text, [single]$size, [string]$hex, [single]$x, [single]$y, [bool]$bold=$false) {
    $style = [System.Drawing.FontStyle]::Regular
    if ($bold) { $style = [System.Drawing.FontStyle]::Bold }
    $font = [System.Drawing.Font]::new('Segoe UI', $size, $style, [System.Drawing.GraphicsUnit]::Pixel)
    $ink = Brush $hex
    $g.DrawString($text, $font, $ink, $x, $y)
    $font.Dispose(); $ink.Dispose()
}
function Rounded($g, [int]$x, [int]$y, [int]$w, [int]$h, [int]$r, [string]$color) {
    $path = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $d = 2*$r
    $path.AddArc($x,$y,$d,$d,180,90); $path.AddArc($x+$w-$d,$y,$d,$d,270,90)
    $path.AddArc($x+$w-$d,$y+$h-$d,$d,$d,0,90); $path.AddArc($x,$y+$h-$d,$d,$d,90,90)
    $path.CloseFigure(); $ink=Brush $color; $g.FillPath($ink,$path); $ink.Dispose(); $path.Dispose()
}
# Render the existing launcher composition (white background, 16% inset).
$icon = [System.Drawing.Bitmap]::new(512,512,[System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($icon)
$g.Clear([System.Drawing.Color]::White)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$logo = [System.Drawing.Image]::FromFile((Join-Path $projectRoot 'app/src/main/res/drawable-nodpi/bio_logo.png'))
$g.DrawImage($logo,[System.Drawing.Rectangle]::new(82,82,348,348))
$icon.Save((Join-Path $outputFolder 'icon-512.png'),[System.Drawing.Imaging.ImageFormat]::Png)
$logo.Dispose(); $g.Dispose(); $icon.Dispose()
# A typographic feature graphic, not a fabricated application screenshot.
$banner = [System.Drawing.Bitmap]::new(1024,500,[System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
$g = [System.Drawing.Graphics]::FromImage($banner)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
$g.Clear([System.Drawing.ColorTranslator]::FromHtml('#EEE8FC'))
$decor=Brush '#E1D7F6'; $g.FillEllipse($decor,760,-150,450,450); $decor.Dispose()
$decor=Brush '#DBEFE5'; $g.FillEllipse($decor,-160,370,530,350); $decor.Dispose()
Label $g 'Bio' 25 '#67508E' 76 78 $true
Label $g 'Tu salud,' 55 '#302244' 72 132 $true
Label $g 'en un solo lugar.' 55 '#302244' 72 197 $true
Label $g 'Organiza tu información de salud' 25 '#665977' 76 297
Label $g 'y conoce tu mapa corporal.' 25 '#665977' 76 334
Rounded $g 634 91 302 92 23 '#FDFBFF'
Rounded $g 634 204 302 92 23 '#FDFBFF'
Rounded $g 634 317 302 92 23 '#FDFBFF'
$pen=[System.Drawing.Pen]::new([System.Drawing.ColorTranslator]::FromHtml('#7850AD'),3)
$g.DrawRectangle($pen,660,116,29,39); $g.DrawLine($pen,667,128,682,128); $g.DrawLine($pen,667,137,682,137)
$g.DrawRectangle($pen,660,229,29,39); $g.DrawLine($pen,674,238,674,259); $g.DrawLine($pen,665,248,684,248)
$g.DrawEllipse($pen,667,339,16,16); $g.DrawLine($pen,675,355,675,380); $g.DrawLine($pen,659,364,691,364)
$g.DrawLine($pen,675,380,665,390); $g.DrawLine($pen,675,380,685,390)
$pen.Dispose()
Label $g 'Exámenes' 25 '#49355F' 712 117 $true
Label $g 'Historia clínica' 25 '#49355F' 712 230 $true
Label $g 'Mapa corporal' 25 '#49355F' 712 343 $true
$banner.Save((Join-Path $outputFolder 'feature-1024x500.png'),[System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $banner.Dispose()
Get-ChildItem $outputFolder -Filter '*.png' | Select-Object Name,Length
