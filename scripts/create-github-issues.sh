#!/bin/bash
# Script to create GitHub issues for CWT future enhancements
# This script uses the GitHub CLI (gh) to create issues in the VectorWave repository

# Ensure we're in the right repository
if ! git remote get-url origin | grep -q "VectorWave"; then
    echo "Error: This script must be run from the VectorWave repository"
    exit 1
fi

# Check if gh is installed
if ! command -v gh &> /dev/null; then
    echo "Error: GitHub CLI (gh) is not installed. Please install it first:"
    echo "  brew install gh"
    exit 1
fi

# Check if authenticated
if ! gh auth status &> /dev/null; then
    echo "Error: Not authenticated with GitHub. Please run:"
    echo "  gh auth login"
    exit 1
fi

# Get repository info
REPO=$(gh repo view --json nameWithOwner -q .nameWithOwner)
echo "Creating issues in repository: $REPO"

# Function to create issue from markdown file
create_issue() {
    local file=$1
    local title=$2
    local labels=$3
    local milestone=$4
    
    echo "Creating issue: $title"
    
    # Extract body content (everything except the title line and metadata)
    local body=$(sed -n '3,/^## Labels/p' "$file" | sed '$d')
    
    # Create the issue
    gh issue create \
        --title "$title" \
        --body "$body" \
        --label "$labels" \
        --milestone "$milestone" \
        --project "MorphIQ" \
        2>/dev/null || echo "  Note: Could not add to MorphIQ project (may need permissions)"
}

# Create milestone if it doesn't exist
echo "Checking milestones..."
if ! gh api repos/:owner/:repo/milestones --jq '.[] | select(.title == "CWT Future Enhancements")' | grep -q "CWT Future Enhancements"; then
    echo "Creating milestone: CWT Future Enhancements"
    gh api repos/:owner/:repo/milestones \
        --method POST \
        --field title="CWT Future Enhancements" \
        --field description="Future enhancements for the Continuous Wavelet Transform module" \
        --field state="open"
fi

# Create issues
echo ""
echo "Creating issues..."

# Issue 1: Scalogram Visualization
create_issue \
    "github-issues/cwt-scalogram-visualization.md" \
    "Scalogram Visualization and Export Tools" \
    "enhancement,visualization,cwt" \
    "CWT Future Enhancements"

# Issue 2: Wavelet Coherence
create_issue \
    "github-issues/cwt-wavelet-coherence.md" \
    "Wavelet Coherence and Cross-Wavelet Analysis" \
    "enhancement,analysis,algorithms,cwt" \
    "CWT Future Enhancements"

# Issue 3: Significance Testing
create_issue \
    "github-issues/cwt-significance-testing.md" \
    "Statistical Significance Testing for CWT" \
    "enhancement,statistics,analysis,cwt" \
    "CWT Future Enhancements"

# Issue 4: Streaming CWT
create_issue \
    "github-issues/cwt-streaming-implementation.md" \
    "Streaming CWT Implementation" \
    "enhancement,performance,real-time,cwt" \
    "CWT Future Enhancements"

# Issue 5: GPU Acceleration
create_issue \
    "github-issues/cwt-gpu-acceleration.md" \
    "GPU Acceleration for CWT" \
    "enhancement,performance,gpu,cwt" \
    "CWT Future Enhancements"

# Issue 6: Wavelet Packet Transform
create_issue \
    "github-issues/wavelet-packet-transform.md" \
    "Wavelet Packet Transform Implementation" \
    "enhancement,algorithms,advanced,cwt" \
    "CWT Future Enhancements"

# Issue 7: Time-Scale Analysis Tools
create_issue \
    "github-issues/time-scale-analysis-tools.md" \
    "Time-Scale Analysis Tools" \
    "enhancement,analysis,algorithms,time-frequency,advanced" \
    "CWT Future Enhancements"

# Issue 8: Biomedical Signal Analysis
create_issue \
    "github-issues/biomedical-signal-analysis.md" \
    "Biomedical Signal Analysis Module" \
    "enhancement,biomedical,healthcare,domain-specific,algorithms" \
    "CWT Future Enhancements"

# Issue 9: Audio/Speech Processing
create_issue \
    "github-issues/audio-speech-processing.md" \
    "Audio and Speech Processing Module" \
    "enhancement,audio,speech,music,real-time,domain-specific" \
    "CWT Future Enhancements"

# Issue 10: Geophysical Applications
create_issue \
    "github-issues/geophysical-applications.md" \
    "Geophysical Signal Analysis Module" \
    "enhancement,geophysics,climate,seismic,oceanography,domain-specific" \
    "CWT Future Enhancements"

# Issue 11: Overview/Tracking Issue
create_issue \
    "github-issues/cwt-future-enhancements-overview.md" \
    "CWT Future Enhancements - Overview and Tracking" \
    "enhancement,cwt,roadmap,tracking,pinned" \
    "CWT Future Enhancements"

echo ""
echo "Done! All issues have been created."
echo ""
echo "To view the issues:"
echo "  gh issue list --label cwt"
echo ""
echo "To add issues to the MorphIQ project manually:"
echo "  1. Go to the project page"
echo "  2. Click 'Add items'"
echo "  3. Search for 'label:cwt'"
echo "  4. Select all issues and add them"