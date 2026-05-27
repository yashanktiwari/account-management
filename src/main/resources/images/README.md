# Invoice Header and Footer Images

This folder contains the header and footer images used in invoice PDF generation.

## Required Files

Place the following image files in this directory:

1. **HEADER** (with any extension: .png, .jpg, .jpeg)
   - This image will be displayed at the top of the invoice PDF
   - Should contain company logo, name, PAN, and GSTIN
   - Recommended dimensions: Width should match A4 paper width (approximately 595 pixels)
   - Maximum height: 100 pixels
   - Example: HEADER.jpg, HEADER.png, HEADER.jpeg

2. **FOOTER** (with any extension: .png, .jpg, .jpeg)
   - This image will be displayed at the bottom of the invoice PDF
   - Should contain company address, email, and contact information
   - Recommended dimensions: Width should match A4 paper width (approximately 595 pixels)
   - Maximum height: 80 pixels
   - Example: FOOTER.jpg, FOOTER.png, FOOTER.jpeg

## Image Format

- Supported formats: PNG, JPG, JPEG
- File extensions are case-insensitive (HEADER.jpg, HEADER.JPG, Header.jpg all work)

## Fallback Behavior

If the images are not found, the system will automatically use text-based header and footer as a fallback.

## Notes

- Images will be automatically scaled to fit the page width while maintaining aspect ratio
- For best results, use high-resolution images (300 DPI or higher)
- Ensure images have transparent backgrounds if needed
